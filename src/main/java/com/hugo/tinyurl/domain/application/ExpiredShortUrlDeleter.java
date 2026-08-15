package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.domain.port.ClickEventRepository;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ExpiredShortUrlDeleter {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final ClickCountRepository clickCountRepository;

    @Transactional
    void deleteAll(List<Long> shortUrlIds, List<ClickEvent> clickEvents) {
        clickEventRepository.deleteAll(clickEvents);
        clickCountRepository.deleteAllById(shortUrlIds);
        shortUrlRepository.deleteAllById(shortUrlIds);
    }

}
