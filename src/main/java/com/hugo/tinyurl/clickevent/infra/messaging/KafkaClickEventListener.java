package com.hugo.tinyurl.clickevent.infra.messaging;

import com.hugo.tinyurl.clickevent.application.ClickEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class KafkaClickEventListener {

    private final ClickEventService clickEventService;

    @KafkaListener(
        topics = "${app.click-event.kafka.topic}",
        groupId = "${app.click-event.kafka.consumer-group}",
        concurrency = "${app.click-event.kafka.concurrency}"
    )
    void listen(ClickEventMessage message, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.OFFSET) long offset) {
        String deliveryKey = partition + "-" + offset;
        clickEventService.record(message.shortUrlId(), message.ipAddress(), message.userAgent(), message.referer(), deliveryKey);
    }

}
