package com.hugo.tinyurl.infra.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Slf4j
@Configuration
class ClickEventConsumerErrorHandlerConfig {

    @Bean
    CommonErrorHandler clickEventConsumerErrorHandler(
        KafkaTemplate<String, ClickEventMessage> kafkaTemplate,
        MeterRegistry meterRegistry,
        @Value("${app.click-event.kafka.retry.initial-interval-ms}") long initialIntervalMs,
        @Value("${app.click-event.kafka.retry.max-interval-ms}") long maxIntervalMs,
        @Value("${app.click-event.kafka.retry.max-elapsed-ms}") long maxElapsedMs
    ) {
        Counter consumeFailureCounter = Counter.builder("click_event.consume.failure").register(meterRegistry);

        // 재시도 중간이 아니라 최종적으로 DLQ로 넘어가는 시점에만 카운트/로그해야 지표가 부풀려지지 않는다.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (record, ex) -> {
                String deliveryKey = record.partition() + "-" + record.offset();
                consumeFailureCounter.increment();
                log.error("클릭 이벤트 재시도 소진, DLQ로 전달 - deliveryKey={}", deliveryKey, ex);
                return new TopicPartition(record.topic() + KafkaTopicConfig.DEAD_LETTER_TOPIC_SUFFIX, record.partition());
            });

        ExponentialBackOff backOff = new ExponentialBackOff(initialIntervalMs, ExponentialBackOff.DEFAULT_MULTIPLIER);
        backOff.setMaxInterval(maxIntervalMs);
        backOff.setMaxElapsedTime(maxElapsedMs);

        // blocking retry라 재시도 중 같은 파티션의 다음 이벤트도 지연되지만, non-blocking(별도 retry topic)보다 단순함을 택했다.
        return new DefaultErrorHandler(recoverer, backOff);
    }

}
