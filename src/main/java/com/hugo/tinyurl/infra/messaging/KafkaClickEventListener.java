package com.hugo.tinyurl.infra.messaging;

import com.hugo.tinyurl.domain.application.ClickEventService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class KafkaClickEventListener {

    private final ClickEventService clickEventService;
    private final Counter consumeFailureCounter;

    KafkaClickEventListener(ClickEventService clickEventService, MeterRegistry meterRegistry) {
        this.clickEventService = clickEventService;
        this.consumeFailureCounter = Counter.builder("click_event.consume.failure").register(meterRegistry);
    }

    @KafkaListener(
        topics = "${app.click-event.kafka.topic}",
        groupId = "${app.click-event.kafka.consumer-group}",
        concurrency = "${app.click-event.kafka.concurrency}"
    )
    void listen(ClickEventMessage message, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.OFFSET) long offset) {
        String deliveryKey = partition + "-" + offset;
        try {
            clickEventService.record(message.shortUrlId(), message.ipAddress(), message.userAgent(), message.referer(), deliveryKey);
        } catch (Exception e) {
            consumeFailureCounter.increment();
            log.error("클릭 이벤트 기록 실패 - deliveryKey={}", deliveryKey, e);
        }
    }

}
