package com.hugo.tinyurl.clickevent.infra.messaging;

import com.hugo.tinyurl.clickevent.port.ClickEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class KafkaClickEventPublisher implements ClickEventPublisher {

    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;
    private final String topic;
    private final Counter publishFailureCounter;

    KafkaClickEventPublisher(
        KafkaTemplate<String, ClickEventMessage> kafkaTemplate,
        MeterRegistry meterRegistry,
        @Value("${app.click-event.kafka.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.publishFailureCounter = Counter.builder("click_event.publish.failure").register(meterRegistry);
    }

    @Override
    public void publish(Long shortUrlId, String ipAddress, String userAgent, String referer) {
        ClickEventMessage message = new ClickEventMessage(shortUrlId, ipAddress, userAgent, referer);
        // send()는 브로커 통신 실패를 대부분 whenComplete의 비동기 콜백으로만 알려준다 -
        // 카운터를 여기 두지 않으면 실제 발행 실패를 거의 못 잡는다.
        try {
            kafkaTemplate.send(topic, shortUrlId.toString(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        publishFailureCounter.increment();
                        log.error("클릭 이벤트 발행 실패 - shortUrlId={}", shortUrlId, ex);
                    }
                });
        } catch (Exception e) {
            publishFailureCounter.increment();
            throw e;
        }
    }

}
