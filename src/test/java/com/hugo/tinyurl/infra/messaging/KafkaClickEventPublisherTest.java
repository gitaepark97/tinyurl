package com.hugo.tinyurl.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.port.ClickEventPublisher;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class KafkaClickEventPublisherTest {

    @Autowired
    ClickEventPublisher clickEventPublisher;

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
        consumer = newConsumer();
        consumer.subscribe(List.of(topic));

        clickEventPublisher.publish(1L, "127.0.0.1", "test-agent", "https://referer.example.com");

        ConsumerRecord<String, String> record = pollSingleRecord();
        assertThat(record.key()).isEqualTo("1");
        assertThat(record.value())
            .contains("\"shortUrlId\":1")
            .contains("\"ipAddress\":\"127.0.0.1\"")
            .contains("\"userAgent\":\"test-agent\"")
            .contains("\"referer\":\"https://referer.example.com\"");
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

    private ConsumerRecord<String, String> pollSingleRecord() {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            var records = consumer.poll(Duration.ofMillis(200));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }
        throw new AssertionError("토픽에서 메시지를 받지 못했습니다 - topic=" + topic);
    }

}
