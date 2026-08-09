package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.domain.port.ClickEventRepository;
import com.hugo.tinyurl.domain.port.ClockProvider;
import com.hugo.tinyurl.domain.port.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
class ClickEventRecorder {

    private final ClickEventRepository clickEventRepository;
    private final ClickCountRepository clickCountRepository;
    private final ClockProvider clockProvider;
    private final IdGenerator idGenerator;

    // deliveryKey 유니크 제약 충돌(진짜 동시 중복 처리)도 이 재시도 대상에 걸리는데, 상대 트랜잭션이
    // 재시도 3회 안에 커밋 못 하면 예외가 그대로 던져질 수 있다 - 그래도 KafkaClickEventListener가
    // 잡아서 로그만 남기므로 컨슈머가 멈추진 않는다(클릭 이벤트 하나 유실은 감수).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        includes = {TransientDataAccessException.class, DataIntegrityViolationException.class},
        maxRetries = 2, delay = 200, multiplier = 2
    )
    void record(Long shortUrlId, String ipAddress, String userAgent, String referer, String deliveryKey) {
        if (clickEventRepository.existsByDeliveryKey(deliveryKey)) {
            log.info("중복 전달된 클릭 이벤트 스킵 - deliveryKey={}", deliveryKey);
            return;
        }
        ClickEvent clickEvent =
            ClickEvent.create(idGenerator.generate(), shortUrlId, ipAddress, userAgent, referer, deliveryKey, clockProvider.now());
        clickEventRepository.save(clickEvent);
        clickCountRepository.increment(shortUrlId);
    }

}
