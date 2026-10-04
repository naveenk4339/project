package com.pos.inventory;

import com.pos.common.events.EventEnvelope;
import com.pos.common.events.EventJson;
import com.pos.common.events.EventTypes;
import com.pos.common.events.PaymentSummary;
import com.pos.common.events.Topics;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = Topics.TRANSACTION_EVENTS)
class InventoryConsumerTest {

    @Autowired
    KafkaTemplate<String, String> kafka;
    @Autowired
    StockRepository stock;

    @Test
    void decrementsStockOnceEvenWhenEventIsRedelivered() {
        int before = stock.findById("ELE-002").orElseThrow().getOnHand();
        TransactionCompleted completed = new TransactionCompleted("TX-1", "store-001", "t1", null,
                List.of(new TransactionLine("ELE-002", "Earbuds", "electronics", 2, 4999, 0, 9998)),
                9998, 0, 600, 10598, List.of(),
                new PaymentSummary("p1", "CARD", "VISA", "4242", "fp", "123456", 10598, 10598, 0, 0.1), Instant.now());
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), EventTypes.TRANSACTION_COMPLETED,
                "PosTransaction", "TX-1", Instant.now(), EventJson.mapper().valueToTree(completed));
        String json = EventJson.write(envelope);

        kafka.send(Topics.TRANSACTION_EVENTS, "TX-1", json);
        kafka.send(Topics.TRANSACTION_EVENTS, "TX-1", json); // at-least-once redelivery
        kafka.send(Topics.TRANSACTION_EVENTS, "bad", "{not json"); // poison message must not block the partition

        await().untilAsserted(() -> assertThat(stock.findById("ELE-002").orElseThrow().getOnHand()).isEqualTo(before - 2));
        // give the duplicate time to be (not) applied
        await().pollDelay(java.time.Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(stock.findById("ELE-002").orElseThrow().getOnHand()).isEqualTo(before - 2));
    }
}
