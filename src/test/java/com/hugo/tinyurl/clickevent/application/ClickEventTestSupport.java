package com.hugo.tinyurl.clickevent.application;

import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.clickevent.port.ClickEventRepository;
import java.util.List;

public final class ClickEventTestSupport {

    private ClickEventTestSupport() {
    }

    public static List<ClickEvent> findAllByShortUrlId(ClickEventRepository clickEventRepository, long shortUrlId) {
        return clickEventRepository.findByShortUrlIdAndIdLessThanOrderByIdDesc(shortUrlId, Long.MAX_VALUE, Integer.MAX_VALUE);
    }

}
