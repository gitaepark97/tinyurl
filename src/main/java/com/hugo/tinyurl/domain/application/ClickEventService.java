package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.domain.model.Role;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClickEventService {

    private final ClickEventFinder clickEventFinder;
    private final ClickEventRecorder clickEventRecorder;

    public Page<ClickEvent> findAll(Long shortUrlId, Long requesterMemberId, Role requesterRole, PageParam pageParam) {
        return clickEventFinder.findAll(shortUrlId, requesterMemberId, requesterRole, pageParam);
    }

    public void record(Long shortUrlId, String ipAddress, String userAgent, String referer, String deliveryKey) {
        clickEventRecorder.record(shortUrlId, ipAddress, userAgent, referer, deliveryKey);
    }

}
