package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.entity.ClickEvent;
import com.hugo.tinyurl.domain.repository.ClickCountRepository;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(includes = TransientDataAccessException.class, maxRetries = 2, delay = 200, multiplier = 2)
    void record(Long shortUrlId, String ipAddress, String userAgent, String referer) {
        clickEventRepository.save(new ClickEvent(shortUrlId, ipAddress, userAgent, referer));
        clickCountRepository.increment(shortUrlId);
    }

}
