package com.hugo.tinyurl.clickevent.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.clickevent.port.ClickEventPublisher;
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
class KafkaClickEventPublisherTest {

    @Autowired
    ClickEventPublisher clickEventPublisher;

    @Autowired
    Tracer tracer;

    @Autowired
    ObservationRegistry observationRegistry;

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
    void publishesMessageKeyedByShortUrlId() {
        long shortUrlId = System.nanoTime();
        consumer = newConsumer();
        consumer.subscribe(List.of(topic));

        clickEventPublisher.publish(shortUrlId, "127.0.0.1", "test-agent", "https://referer.example.com");

        // 같은 토픽을 다른 테스트도 공유하므로(Testcontainers Kafka가 전체 테스트 실행 동안 하나만 뜬다),
        // earliest부터 읽되 이번에 발행한 shortUrlId를 key로 가진 레코드가 나올 때까지 걸러낸다.
        ConsumerRecord<String, String> record = pollUntilKeyMatches(String.valueOf(shortUrlId));
        assertThat(record.value())
            .contains("\"shortUrlId\":" + shortUrlId)
            .contains("\"ipAddress\":\"127.0.0.1\"")
            .contains("\"userAgent\":\"test-agent\"")
            .contains("\"referer\":\"https://referer.example.com\"");
    }

    @Test
    void propagatesTraceContextIntoMessageHeaders() {
        long shortUrlId = System.nanoTime();
        consumer = newConsumer();
        consumer.subscribe(List.of(topic));

        // 부모-자식 링크는 현재 Observation 기준이라 Tracer.withSpan이 아닌 Observation을 직접 열어야 한다.
        Observation observation = Observation.createNotStarted("test-publish", observationRegistry).start();
        String traceId;
        try (Observation.Scope ignored = observation.openScope()) {
            traceId = tracer.currentSpan().context().traceId();
            clickEventPublisher.publish(shortUrlId, "127.0.0.1", "test-agent", "https://referer.example.com");
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
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "click-event-publisher-test-" + System.nanoTime());
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
