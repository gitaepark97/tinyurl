package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.Role;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.model.ShortUrlWithClickCount;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Observed
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private final ShortUrlManager shortUrlManager;
    private final ShortUrlFinder shortUrlFinder;
    private final ClickEventManager clickEventManager;

    public ShortUrl create(String originalUrl) {
        return shortUrlManager.create(null, originalUrl, null, null);
    }

    public ShortUrl create(Long memberId, String originalUrl, String customAlias, LocalDateTime expiresAt) {
        return shortUrlManager.create(memberId, originalUrl, customAlias, expiresAt);
    }

    public Page<ShortUrlWithClickCount> findAll(PageParam pageParam) {
        return shortUrlFinder.findAll(pageParam);
    }

    public Page<ShortUrlWithClickCount> findAllByMember(Long memberId, PageParam pageParam) {
        return shortUrlFinder.findAllByMember(memberId, pageParam);
    }

    public ShortUrlWithClickCount find(Long id, Long requesterMemberId, Role requesterRole) {
        return shortUrlFinder.get(id, requesterMemberId, requesterRole);
    }

    public String redirect(String shortKey, String ipAddress, String userAgent, String referer) {
        ShortUrl shortUrl = shortUrlFinder.find(shortKey);
        try {
            clickEventManager.record(shortUrl.id(), ipAddress, userAgent, referer);
        } catch (Exception e) {
            log.error("클릭 이벤트 기록 실패 - shortUrlId={}", shortUrl.id(), e);
        }
        return shortUrl.originalUrl();
    }

}
