package com.hugo.tinyurl;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

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

}
