package com.pos.relay;

import com.pos.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfig {

    @Bean
    NewTopic transactionEvents() {
        return TopicBuilder.name(Topics.TRANSACTION_EVENTS).partitions(6).replicas(1).build();
    }

    @Bean
    NewTopic transactionEventsDlt() {
        return TopicBuilder.name(Topics.TRANSACTION_EVENTS_DLT).partitions(1).replicas(1).build();
    }
}
