package com.hugo.tinyurl.clickevent.application;

import com.hugo.tinyurl.clickevent.port.ClickEventPublisher;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Observed
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventManager {

    private final ClickEventPublisher clickEventPublisher;

    // 예외를 삼키지 않고 그대로 던진다 - 호출자(ClickEventVisitListener)가 실패해야
    // Spring Modulith가 이벤트 발행을 미완료로 기록하고 재시도 대상으로 남긴다.
    public void record(Long shortUrlId, String ipAddress, String userAgent, String referer) {
        try {
            clickEventPublisher.publish(shortUrlId, ipAddress, userAgent, referer);
        } catch (Exception e) {
            log.error("클릭 이벤트 발행 실패 - shortUrlId={}", shortUrlId, e);
            throw e;
        }
    }

}
