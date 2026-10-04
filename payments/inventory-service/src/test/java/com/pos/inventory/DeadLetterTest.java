package com.pos.inventory;

import com.pos.common.events.Topics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {Topics.TRANSACTION_EVENTS, Topics.TRANSACTION_EVENTS_DLT})
class DeadLetterTest {

    @Autowired
    KafkaTemplate<String, String> kafka;
    @Autowired
    EmbeddedKafkaBroker broker;

    @Test
    void malformedEventIsParkedOnTheDeadLetterTopic() {
        kafka.send(Topics.TRANSACTION_EVENTS, "bad", "{not json");

        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps("dlt-test", "true", broker),
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, Topics.TRANSACTION_EVENTS_DLT);
            ConsumerRecord<String, String> dead = KafkaTestUtils.getSingleRecord(consumer, Topics.TRANSACTION_EVENTS_DLT, Duration.ofSeconds(15));
            assertThat(dead.value()).isEqualTo("{not json");
            assertThat(dead.headers().lastHeader("kafka_dlt-exception-message")).isNotNull();
        }
    }
}
