package com.pos.common.kafka;

import com.pos.common.events.MalformedEventException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Consumer error handling shared by every downstream service: retry transient failures with
 * exponential back-off, then park the record on {@code <topic>.DLT}. Poison messages skip the retries.
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaOperations.class)
public class KafkaConsumerDefaults {

    @Bean
    @ConditionalOnBean(KafkaOperations.class)
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public CommonErrorHandler posKafkaErrorHandler(KafkaOperations<?, ?> kafkaOperations) {
        ExponentialBackOff backOff = new ExponentialBackOff(500, 2.0);
        backOff.setMaxElapsedTime(10_000);
        // explicit destination: the library default is "<topic>-dlt", we provision and document "<topic>.DLT"
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", -1));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(MalformedEventException.class, IllegalArgumentException.class);
        return handler;
    }
}
