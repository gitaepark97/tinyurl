package com.hugo.tinyurl.clickevent;

import com.hugo.tinyurl.clickevent.application.ClickEventFinder;
import com.hugo.tinyurl.clickevent.application.ClickEventRecorder;
import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.common.page.Page;
import com.hugo.tinyurl.common.page.PageParam;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Observed
@Service
@RequiredArgsConstructor
public class ClickEventService {

    private final ClickEventFinder clickEventFinder;
    private final ClickEventRecorder clickEventRecorder;

    // shortUrlId에 대한 조회 권한(소유자/관리자) 확인은 호출자(shorturl 모듈)의 책임이다 -
    // clickevent는 인증 정보 없이 순수하게 클릭 이벤트만 돌려준다.
    public Page<ClickEvent> findAll(Long shortUrlId, PageParam pageParam) {
        return clickEventFinder.findAll(shortUrlId, pageParam);
    }

    public void record(Long shortUrlId, String ipAddress, String userAgent, String referer, String deliveryKey) {
        clickEventRecorder.record(shortUrlId, ipAddress, userAgent, referer, deliveryKey);
    }

}
