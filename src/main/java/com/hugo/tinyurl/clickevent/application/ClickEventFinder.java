package com.hugo.tinyurl.clickevent.application;

import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.clickevent.port.ClickEventRepository;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Observed
@Component
@RequiredArgsConstructor
public class ClickEventFinder {

    private final ClickEventRepository clickEventRepository;

    @Transactional(readOnly = true)
    public Page<ClickEvent> findAll(Long shortUrlId, PageParam pageParam) {
        List<ClickEvent> overFetched = clickEventRepository.findByShortUrlIdAndIdLessThanOrderByIdDesc(
            shortUrlId, pageParam.cursorOrInitial(), pageParam.size() + 1);

        boolean hasNext = overFetched.size() > pageParam.size();
        List<ClickEvent> events = hasNext ? overFetched.subList(0, pageParam.size()) : overFetched;
        return Page.of(events, hasNext);
    }

}
