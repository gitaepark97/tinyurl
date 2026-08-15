package com.hugo.tinyurl.infra.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
class KafkaTopicConfig {

    static final String DEAD_LETTER_TOPIC_SUFFIX = ".DLT";

    @Bean
    NewTopic clickEventsTopic(
        @Value("${app.click-event.kafka.topic}") String topic,
        @Value("${app.click-event.kafka.partitions}") int partitions
    ) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1).build();
    }

    @Bean
    NewTopic clickEventsDeadLetterTopic(
        @Value("${app.click-event.kafka.topic}") String topic,
        @Value("${app.click-event.kafka.partitions}") int partitions
    ) {
        // 원본과 파티션 수를 맞춰야 DeadLetterPublishingRecoverer가 원본 파티션 번호를 그대로 재사용할 수 있다.
        return TopicBuilder.name(topic + DEAD_LETTER_TOPIC_SUFFIX).partitions(partitions).replicas(1).build();
    }

}
