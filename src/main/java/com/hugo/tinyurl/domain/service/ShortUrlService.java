package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.dto.ShortUrlWithClickCount;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private final ShortUrlManager shortUrlManager;
    private final ShortUrlFinder shortUrlFinder;
    private final ClickEventManager clickEventManager;

    public ShortUrl create(String originalUrl) {
        return shortUrlManager.create(originalUrl);
    }

    public Page<ShortUrlWithClickCount> findAll(PageParam pageParam) {
        return shortUrlFinder.findAll(pageParam);
    }

    public ShortUrlWithClickCount find(Long id) {
        return shortUrlFinder.get(id);
    }

    public String redirect(String shortKey, String ipAddress, String userAgent, String referer) {
        ShortUrl shortUrl = shortUrlFinder.find(shortKey);
        try {
            clickEventManager.record(shortUrl.getId(), ipAddress, userAgent, referer);
        } catch (Exception e) {
            log.error("클릭 이벤트 기록 실패 - shortUrlId={}", shortUrl.getId(), e);
        }
        return shortUrl.getOriginalUrl();
    }

}
