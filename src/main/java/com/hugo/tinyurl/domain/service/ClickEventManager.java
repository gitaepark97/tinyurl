package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.entity.ClickEvent;
import com.hugo.tinyurl.domain.repository.ClickCountRepository;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ClickEventManager {

    private final ClickEventRepository clickEventRepository;
    private final ClickCountRepository clickCountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void record(Long shortUrlId, String ipAddress, String userAgent, String referer) {
        clickEventRepository.save(new ClickEvent(shortUrlId, ipAddress, userAgent, referer));
        clickCountRepository.increment(shortUrlId);
    }

}
