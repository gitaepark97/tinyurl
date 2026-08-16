package com.hugo.tinyurl;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final String TEST_ARCHIVE_BUCKET = "tinyurl-expired-archive-test";

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4.10"));
    }

    @Bean
    @ServiceConnection
    RedisContainer redisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:8.2-alpine"));
    }

    @Bean
    GenericContainer<?> zookeeperContainer() {
        return new GenericContainer<>(DockerImageName.parse("zookeeper:3.9.4")).withExposedPorts(2181);
    }

    @Bean
    DynamicPropertyRegistrar zookeeperProperties(GenericContainer<?> zookeeperContainer) {
        return registry -> registry.add(
            "app.zookeeper.connect-string",
            () -> zookeeperContainer.getHost() + ":" + zookeeperContainer.getMappedPort(2181)
        );
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        // apache/kafka:3.9.0은 advertised.listeners 검증 실패로 기동이 안 돼 3.7.0을 쓴다.
        return new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));
    }

    @Bean
    LocalStackContainer localStackContainer() {
        return new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8")).withServices(Service.S3);
    }

    @Bean
    DynamicPropertyRegistrar s3Properties(LocalStackContainer localStackContainer) throws Exception {
        // 버킷은 LocalStack init 스크립트가 아니라 여기서 직접 만든다 - 테스트 전용 버킷 이름을 앱 설정과 분리하기 위함.
        localStackContainer.execInContainer("awslocal", "s3", "mb", "s3://" + TEST_ARCHIVE_BUCKET);
        return registry -> {
            registry.add("app.storage.s3.endpoint", () -> localStackContainer.getEndpointOverride(Service.S3).toString());
            registry.add("app.storage.s3.region", localStackContainer::getRegion);
            registry.add("app.storage.s3.bucket", () -> TEST_ARCHIVE_BUCKET);
        };
    }

}
