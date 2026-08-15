package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.domain.port.ClickEventRepository;
import com.hugo.tinyurl.domain.port.ClockProvider;
import com.hugo.tinyurl.domain.port.IdGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Observed
@Slf4j
@Component
class ClickEventRecorder {

    private final ClickEventRepository clickEventRepository;
    private final ClickCountRepository clickCountRepository;
    private final ClockProvider clockProvider;
    private final IdGenerator idGenerator;
    private final Counter duplicateCounter;

    ClickEventRecorder(
        ClickEventRepository clickEventRepository,
        ClickCountRepository clickCountRepository,
        ClockProvider clockProvider,
        IdGenerator idGenerator,
        MeterRegistry meterRegistry
    ) {
        this.clickEventRepository = clickEventRepository;
        this.clickCountRepository = clickCountRepository;
        this.clockProvider = clockProvider;
        this.idGenerator = idGenerator;
        this.duplicateCounter = Counter.builder("click_event.duplicate").register(meterRegistry);
    }

    // deliveryKey 유니크 제약 충돌(동시 중복 처리)을 흡수하기 위한 짧은 재시도 - 그래도 실패하면 컨테이너 레벨 재시도/DLQ로 넘어간다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        includes = {TransientDataAccessException.class, DataIntegrityViolationException.class},
        maxRetries = 2, delay = 200, multiplier = 2
    )
    void record(Long shortUrlId, String ipAddress, String userAgent, String referer, String deliveryKey) {
        if (clickEventRepository.existsByDeliveryKey(deliveryKey)) {
            duplicateCounter.increment();
            log.info("중복 전달된 클릭 이벤트 스킵 - deliveryKey={}", deliveryKey);
            return;
        }
        ClickEvent clickEvent =
            ClickEvent.create(idGenerator.generate(), shortUrlId, ipAddress, userAgent, referer, deliveryKey, clockProvider.now());
        clickEventRepository.save(clickEvent);
        clickCountRepository.increment(shortUrlId);
    }

}
