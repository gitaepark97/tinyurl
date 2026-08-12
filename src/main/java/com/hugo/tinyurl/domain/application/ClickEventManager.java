package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.port.ClickEventPublisher;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Observed
@Slf4j
@Component
@RequiredArgsConstructor
class ClickEventManager {

    private final ClickEventPublisher clickEventPublisher;
    private final ObservationRegistry observationRegistry;

    @Async("clickEventPublishExecutor")
    void record(Long shortUrlId, String ipAddress, String userAgent, String referer) {
        try {
            clickEventPublisher.publish(shortUrlId, ipAddress, userAgent, referer);
        } catch (Exception e) {
            // 예외를 여기서 삼키므로 @Observed span에 발행 실패가 남도록 직접 기록한다.
            Observation observation = observationRegistry.getCurrentObservation();
            if (observation != null) {
                observation.error(e);
            }
            log.error("클릭 이벤트 발행 실패 - shortUrlId={}", shortUrlId, e);
        }
    }

}
