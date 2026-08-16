package com.hugo.tinyurl.shorturl.application;

import com.hugo.tinyurl.clickevent.port.ClickCountRepository;
import com.hugo.tinyurl.clickevent.port.ClickEventRepository;
import com.hugo.tinyurl.shorturl.port.ShortUrlRepository;
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
    void deleteAll(List<Long> shortUrlIds) {
        clickEventRepository.deleteAllByShortUrlIdIn(shortUrlIds);
        clickCountRepository.deleteAllById(shortUrlIds);
        shortUrlRepository.deleteAllById(shortUrlIds);
    }

}
