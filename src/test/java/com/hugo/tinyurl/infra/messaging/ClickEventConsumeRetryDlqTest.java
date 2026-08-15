package com.hugo.tinyurl.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.application.ClickEventService;
import com.hugo.tinyurl.domain.port.ClickEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ClickEventConsumeRetryDlqTest {

    @Autowired
    ClickEventPublisher clickEventPublisher;

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    KafkaContainer kafkaContainer;

    @Value("${app.click-event.kafka.topic}")
    String topic;

    @MockitoBean
    ClickEventService clickEventService;

    @Test
    void publishesToDeadLetterTopicAndIncrementsCounterAfterRetriesExhausted() {
        Long shortUrlId = 3_000_000L;
        willThrow(new RuntimeException("db down")).given(clickEventService)
            .record(any(), any(), any(), any(), any());
        double before = meterRegistry.get("click_event.consume.failure").counter().count();

        clickEventPublisher.publish(shortUrlId, "127.0.0.1", "test-agent", null);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            assertThat(meterRegistry.get("click_event.consume.failure").counter().count()).isEqualTo(before + 1));
        assertThat(pollDeadLetterTopicForShortUrlId(shortUrlId)).isTrue();
    }

    private boolean pollDeadLetterTopicForShortUrlId(Long shortUrlId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-test-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic + KafkaTopicConfig.DEAD_LETTER_TOPIC_SUFFIX));
            long deadline = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                boolean found = false;
                for (ConsumerRecord<String, String> record : records) {
                    found |= record.value().contains(shortUrlId.toString());
                }
                if (found) {
                    return true;
                }
            }
            return false;
        }
    }

}
