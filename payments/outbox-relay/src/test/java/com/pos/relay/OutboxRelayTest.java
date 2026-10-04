package com.pos.relay;

import com.pos.common.events.EventEnvelope;
import com.pos.common.events.EventJson;
import com.pos.common.events.Topics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = Topics.TRANSACTION_EVENTS)
class OutboxRelayTest {

    @Autowired
    OutboxRelay relay;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EmbeddedKafkaBroker broker;

    @Test
    void publishesPendingRowsInOrderAndMarksThemPublished() {
        UUID first = insert("TX-1", "TransactionCompleted", "{\"transactionId\":\"TX-1\"}", Instant.now().minusSeconds(2));
        UUID second = insert("TX-1", "TransactionRefunded", "{\"transactionId\":\"TX-1\"}", Instant.now().minusSeconds(1));

        assertThat(relay.drainBatch()).isEqualTo(2);
        assertThat(relay.drainBatch()).isZero(); // nothing left: no double publish

        Map<String, Object> props = KafkaTestUtils.consumerProps("relay-test", "true", broker);
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, Topics.TRANSACTION_EVENTS);
            List<ConsumerRecord<String, String>> received = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 10_000;
            while (received.size() < 2 && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> batch = consumer.poll(Duration.ofMillis(500));
                batch.forEach(received::add);
            }
            assertThat(received).hasSize(2);
            assertThat(received).allSatisfy(r -> assertThat(r.key()).isEqualTo("TX-1"));
            EventEnvelope e1 = EventJson.readEnvelope(received.get(0).value());
            EventEnvelope e2 = EventJson.readEnvelope(received.get(1).value());
            assertThat(e1.eventId()).isEqualTo(first);
            assertThat(e2.eventId()).isEqualTo(second);
            assertThat(e1.payload().get("transactionId").asText()).isEqualTo("TX-1");
        }

        Integer unpublished = jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event WHERE published_at IS NULL", Integer.class);
        assertThat(unpublished).isZero();
    }

    private UUID insert(String aggregateId, String type, String payload, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO outbox_event (id, aggregate_type, aggregate_id, event_type, payload, created_at) VALUES (?,?,?,?,?,?)",
                id, "PosTransaction", aggregateId, type, payload, Timestamp.from(createdAt));
        return id;
    }
}
