package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.domain.port.ClickEventRepository;
import java.util.List;

final class ClickEventTestSupport {

    private ClickEventTestSupport() {
    }

    static List<ClickEvent> findAllByShortUrlId(ClickEventRepository clickEventRepository, long shortUrlId) {
        return clickEventRepository.findByShortUrlIdAndIdLessThanOrderByIdDesc(shortUrlId, Long.MAX_VALUE, Integer.MAX_VALUE);
    }

}
