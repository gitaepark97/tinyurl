package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.entity.ClickEvent;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;

final class ClickEventTestSupport {

    private ClickEventTestSupport() {
    }

    static List<ClickEvent> findAllByShortUrlId(ClickEventRepository clickEventRepository, long shortUrlId) {
        return clickEventRepository.findByShortUrlIdAndIdLessThanOrderByIdDesc(shortUrlId, Long.MAX_VALUE, Pageable.unpaged());
    }

}
