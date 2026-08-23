package com.hugo.tinyurl;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        // apache/kafka:3.9.0은 advertised.listeners 검증 실패로 기동이 안 돼 3.7.0을 쓴다.
        return new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0")).withReuse(true);
    }

}
