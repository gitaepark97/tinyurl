package com.hugo.tinyurl.shorturl.application;

import com.hugo.tinyurl.member.model.Role;
import com.hugo.tinyurl.shorturl.ShortUrlVisitedEvent;
import com.hugo.tinyurl.shorturl.model.ShortUrl;
import com.hugo.tinyurl.shorturl.model.ShortUrlWithClickCount;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private final ApplicationEventPublisher eventPublisher;
    private final ShortUrlManager shortUrlManager;
    private final ShortUrlFinder shortUrlFinder;

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

    // 이벤트 발행이 이벤트 발행 레지스트리에 기록되고 커밋 후에 리스너가 실행되려면
    // 발행 시점에 트랜잭션이 열려 있어야 한다 - 이 메서드 자체는 쓰기가 없지만 그 목적으로 연다.
    @Transactional
    public String redirect(String shortKey, String ipAddress, String userAgent, String referer) {
        ShortUrl shortUrl = shortUrlFinder.find(shortKey);
        try {
            // 이벤트 발행 레지스트리 기록 자체가 실패해도(트랜잭션/DB 문제 등) 리다이렉트는 성공해야 한다 -
            // 다운스트림(Kafka 등) 실패에 대한 재시도 보장은 ClickEventVisitListener 쪽 책임이고,
            // 여기서는 그 이전 단계인 발행 자체의 예외로부터 리다이렉트를 보호한다.
            eventPublisher.publishEvent(new ShortUrlVisitedEvent(shortUrl.id(), ipAddress, userAgent, referer));
        } catch (Exception e) {
            log.error("클릭 이벤트 발행 등록 실패 - shortUrlId={}", shortUrl.id(), e);
        }
        return shortUrl.originalUrl();
    }

}
