package com.hugo.tinyurl.infra.messaging;

import com.hugo.tinyurl.domain.port.ClickEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class KafkaClickEventPublisher implements ClickEventPublisher {

    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;
    private final String topic;

    KafkaClickEventPublisher(
        KafkaTemplate<String, ClickEventMessage> kafkaTemplate,
        @Value("${app.click-event.kafka.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(Long shortUrlId, String ipAddress, String userAgent, String referer) {
        ClickEventMessage message = new ClickEventMessage(shortUrlId, ipAddress, userAgent, referer);
        kafkaTemplate.send(topic, shortUrlId.toString(), message)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("클릭 이벤트 발행 실패 - shortUrlId={}", shortUrlId, ex);
                }
            });
    }

}
