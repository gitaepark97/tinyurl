package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClickEventService {

    private final ClickEventFinder clickEventFinder;

    public Page<ClickEvent> findAll(Long shortUrlId, PageParam pageParam) {
        return clickEventFinder.findAll(shortUrlId, pageParam);
    }

}
