package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.testcontainers.kafka.KafkaContainer;

// SpringBootTest는 기본적으로 트레이싱 export(propagation 포함)를 꺼버리므로 다시 켜야 한다.
@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@AutoConfigureTracing
@Import(TestcontainersConfiguration.class)
class ClickEventManagerContextPropagationTest {

    @Autowired
    Tracer tracer;

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    ClickEventManager clickEventManager;

    @Autowired
    KafkaContainer kafkaContainer;

    @Value("${app.click-event.kafka.topic}")
    String topic;

    KafkaConsumer<String, String> consumer;

    @AfterEach
    void closeConsumer() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void propagatesTraceContextAcrossAsyncBoundaryIntoMessageHeaders() {
        long shortUrlId = System.nanoTime();
        consumer = newConsumer();
        consumer.subscribe(List.of(topic));

        // @Async 경계를 넘어 clickEventPublishExecutor 스레드까지 이 Observation이 전파돼야
        // Kafka publish의 producer span이 자식으로 연결되고 traceId가 헤더에 실린다.
        Observation observation = Observation.createNotStarted("test-record", observationRegistry).start();
        String traceId;
        try (Observation.Scope ignored = observation.openScope()) {
            traceId = tracer.currentSpan().context().traceId();
            clickEventManager.record(shortUrlId, "127.0.0.1", "test-agent", "https://referer.example.com");
        } finally {
            observation.stop();
        }

        ConsumerRecord<String, String> record = pollUntilKeyMatches(String.valueOf(shortUrlId));
        assertThat(record.headers()).anySatisfy(header -> assertThat(headerValue(header)).contains(traceId));
    }

    private String headerValue(Header header) {
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private KafkaConsumer<String, String> newConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "click-event-manager-test-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    private ConsumerRecord<String, String> pollUntilKeyMatches(String expectedKey) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                if (expectedKey.equals(record.key())) {
                    return record;
                }
            }
        }
        throw new AssertionError("토픽에서 key=" + expectedKey + "인 메시지를 받지 못했습니다 - topic=" + topic);
    }

}
