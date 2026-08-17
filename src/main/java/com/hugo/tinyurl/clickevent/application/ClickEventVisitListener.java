package com.hugo.tinyurl.clickevent.application;

import com.hugo.tinyurl.shorturl.ShortUrlVisitedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ClickEventVisitListener {

    private final ClickEventManager clickEventManager;

    @ApplicationModuleListener
    void on(ShortUrlVisitedEvent event) {
        clickEventManager.record(event.shortUrlId(), event.ipAddress(), event.userAgent(), event.referer());
    }

}
