package com.pos.relay;

import com.fasterxml.jackson.databind.JsonNode;
import com.pos.common.events.EventEnvelope;
import com.pos.common.events.EventJson;
import com.pos.common.events.Topics;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Drains {@code outbox_event} into Kafka. Guarantees at-least-once delivery: a row is marked published only
 * after the broker acknowledged it, so a crash in between re-sends it and consumers de-duplicate on eventId.
 * Events are keyed by aggregate id and sent one at a time in creation order, so all events of a transaction
 * land on the same partition in order; if one fails, later events of that aggregate wait for the next poll.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final OutboxProperties properties;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, OutboxProperties properties) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval:500ms}")
    public void poll() {
        int published;
        do {
            published = drainBatch();
        } while (published == properties.batchSize());
    }

    /** Publishes up to one batch inside a single transaction that holds the row locks. Returns rows published. */
    @Transactional
    public int drainBatch() {
        List<Row> rows = jdbc.query("""
                SELECT id, aggregate_type, aggregate_id, event_type, payload, created_at
                FROM outbox_event
                WHERE published_at IS NULL
                ORDER BY created_at
                LIMIT ?
                """ + properties.lockClause(),
                (rs, i) -> new Row(rs.getObject("id", UUID.class), rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"), rs.getString("event_type"), rs.getString("payload"),
                        rs.getTimestamp("created_at").toInstant()),
                properties.batchSize());

        Set<String> blockedAggregates = new HashSet<>();
        int published = 0;
        for (Row row : rows) {
            if (blockedAggregates.contains(row.aggregateId())) {
                continue;
            }
            try {
                send(row);
                jdbc.update("UPDATE outbox_event SET published_at = ?, attempts = attempts + 1 WHERE id = ?",
                        Timestamp.from(Instant.now()), row.id());
                published++;
            } catch (Exception e) {
                blockedAggregates.add(row.aggregateId());
                jdbc.update("UPDATE outbox_event SET attempts = attempts + 1 WHERE id = ?", row.id());
                log.warn("Publishing outbox event {} ({}) failed, will retry: {}", row.id(), row.eventType(), e.toString());
            }
        }
        if (published > 0) {
            log.info("Published {} outbox event(s)", published);
        }
        return published;
    }

    private void send(Row row) throws Exception {
        JsonNode payload = EventJson.mapper().readTree(row.payload());
        EventEnvelope envelope = new EventEnvelope(row.id(), row.eventType(), row.aggregateType(), row.aggregateId(),
                row.createdAt(), payload);
        ProducerRecord<String, String> record =
                new ProducerRecord<>(Topics.TRANSACTION_EVENTS, row.aggregateId(), EventJson.write(envelope));
        record.headers().add("eventType", row.eventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventId", row.id().toString().getBytes(StandardCharsets.UTF_8));
        kafka.send(record).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1M")
    public void purgePublished() {
        int deleted = jdbc.update("DELETE FROM outbox_event WHERE published_at < ?",
                Timestamp.from(Instant.now().minus(properties.retention())));
        if (deleted > 0) {
            log.info("Purged {} published outbox event(s)", deleted);
        }
    }

    private record Row(UUID id, String aggregateType, String aggregateId, String eventType, String payload,
                       Instant createdAt) {
    }
}
