package com.hugo.tinyurl.domain.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ClickEventManager {

    private final ClickEventRecorder clickEventRecorder;

    @Async("clickEventExecutor")
    void record(Long shortUrlId, String ipAddress, String userAgent, String referer) {
        try {
            clickEventRecorder.record(shortUrlId, ipAddress, userAgent, referer);
        } catch (Exception e) {
            log.error("클릭 이벤트 기록 실패 - shortUrlId={}", shortUrlId, e);
        }
    }

}
