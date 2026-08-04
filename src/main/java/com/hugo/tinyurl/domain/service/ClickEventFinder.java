package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.entity.ClickEvent;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ClickEventFinder {

    private final ClickEventRepository clickEventRepository;

    @Transactional(readOnly = true)
    Page<ClickEvent> findAll(Long shortUrlId, PageParam pageParam) {
        List<ClickEvent> overFetched = clickEventRepository.findByShortUrlIdAndIdLessThanOrderByIdDesc(
            shortUrlId, pageParam.cursorOrInitial(), PageRequest.of(0, pageParam.size() + 1));

        boolean hasNext = overFetched.size() > pageParam.size();
        List<ClickEvent> events = hasNext ? overFetched.subList(0, pageParam.size()) : overFetched;
        return Page.of(events, hasNext);
    }

}
