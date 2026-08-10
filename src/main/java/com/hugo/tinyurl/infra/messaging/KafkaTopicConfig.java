package com.hugo.tinyurl.infra.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
class KafkaTopicConfig {

    @Bean
    NewTopic clickEventsTopic(
        @Value("${app.click-event.kafka.topic}") String topic,
        @Value("${app.click-event.kafka.partitions}") int partitions
    ) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1).build();
    }

}
