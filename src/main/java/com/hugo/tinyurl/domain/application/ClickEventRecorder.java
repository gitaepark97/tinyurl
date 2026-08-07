package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.domain.port.ClickEventRepository;
import com.hugo.tinyurl.domain.port.ClockProvider;
import com.hugo.tinyurl.domain.port.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ClickEventRecorder {

    private final ClickEventRepository clickEventRepository;
    private final ClickCountRepository clickCountRepository;
    private final ClockProvider clockProvider;
    private final IdGenerator idGenerator;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(includes = TransientDataAccessException.class, maxRetries = 2, delay = 200, multiplier = 2)
    void record(Long shortUrlId, String ipAddress, String userAgent, String referer) {
        ClickEvent clickEvent = ClickEvent.create(idGenerator.generate(), shortUrlId, ipAddress, userAgent, referer, clockProvider.now());
        clickEventRepository.save(clickEvent);
        clickCountRepository.increment(shortUrlId);
    }

}
