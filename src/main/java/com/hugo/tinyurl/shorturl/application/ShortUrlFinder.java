package com.hugo.tinyurl.shorturl.application;

import com.hugo.tinyurl.clickevent.model.ClickCount;
import com.hugo.tinyurl.clickevent.port.ClickCountRepository;
import com.hugo.tinyurl.common.model.Role;
import com.hugo.tinyurl.common.port.ClockProvider;
import com.hugo.tinyurl.shorturl.model.ShortUrl;
import com.hugo.tinyurl.shorturl.model.ShortUrlWithClickCount;
import com.hugo.tinyurl.shorturl.port.ShortUrlCacheRepository;
import com.hugo.tinyurl.shorturl.port.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Observed
@Component
class ShortUrlFinder {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickCountRepository clickCountRepository;
    private final ClockProvider clockProvider;
    private final ShortUrlCacheRepository shortUrlCacheRepository;
    private final Counter redirectSuccessCounter;
    private final Counter redirectNotFoundCounter;
    private final Counter redirectExpiredCounter;

    ShortUrlFinder(
        ShortUrlRepository shortUrlRepository,
        ClickCountRepository clickCountRepository,
        ClockProvider clockProvider,
        ShortUrlCacheRepository shortUrlCacheRepository,
        MeterRegistry meterRegistry
    ) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickCountRepository = clickCountRepository;
        this.clockProvider = clockProvider;
        this.shortUrlCacheRepository = shortUrlCacheRepository;
        this.redirectSuccessCounter = Counter.builder("short_url.redirect").tag("result", "success").register(meterRegistry);
        this.redirectNotFoundCounter = Counter.builder("short_url.redirect").tag("result", "not_found").register(meterRegistry);
        this.redirectExpiredCounter = Counter.builder("short_url.redirect").tag("result", "expired").register(meterRegistry);
    }

    @Transactional(readOnly = true)
    ShortUrl find(String shortKey) {
        ShortUrl shortUrl = shortUrlCacheRepository.findByShortKey(shortKey, this::findByShortKeyOrNull)
            .orElseGet(() -> {
                redirectNotFoundCounter.increment();
                throw new BusinessException(ErrorCode.NOT_FOUND);
            });

        if (shortUrl.isExpired(clockProvider.now())) {
            shortUrlCacheRepository.evict(shortKey);
            redirectExpiredCounter.increment();
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        redirectSuccessCounter.increment();
        return shortUrl;
    }

    @Transactional(readOnly = true)
    Page<ShortUrlWithClickCount> findAll(PageParam pageParam) {
        List<ShortUrl> overFetched = shortUrlRepository.findByIdLessThanOrderByIdDesc(
            pageParam.cursorOrInitial(), pageParam.size() + 1);
        return toPage(overFetched, pageParam);
    }

    @Transactional(readOnly = true)
    Page<ShortUrlWithClickCount> findAllByMember(Long memberId, PageParam pageParam) {
        List<ShortUrl> overFetched = shortUrlRepository.findByMemberIdAndIdLessThanOrderByIdDesc(
            memberId, pageParam.cursorOrInitial(), pageParam.size() + 1);
        return toPage(overFetched, pageParam);
    }

    @Transactional(readOnly = true)
    ShortUrlWithClickCount get(Long id, Long requesterMemberId, Role requesterRole) {
        ShortUrl shortUrl = findOwnedOrThrow(id, requesterMemberId, requesterRole);
        long clickCount = clickCountRepository.findById(shortUrl.id())
            .map(ClickCount::count)
            .orElse(0L);

        return ShortUrlWithClickCount.of(shortUrl, clickCount);
    }

    // get()과 달리 클릭 수는 조회하지 않는다 - 호출자가 소유자/관리자 여부만 확인하면 되는 경우용.
    @Transactional(readOnly = true)
    void checkAccess(Long id, Long requesterMemberId, Role requesterRole) {
        findOwnedOrThrow(id, requesterMemberId, requesterRole);
    }

    private ShortUrl findOwnedOrThrow(Long id, Long requesterMemberId, Role requesterRole) {
        ShortUrl shortUrl = shortUrlRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (requesterRole != Role.ADMIN && !shortUrl.isOwnedBy(requesterMemberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return shortUrl;
    }

    // 만료 여부는 여기서 걸러내지 않는다 - find()가 not_found/expired를 캐시 신선도와 무관하게
    // 일관되게 구분하려면, 이미 만료된 항목도 일단 반환해서 find()의 isExpired 체크를 항상 거치게 해야 한다.
    private ShortUrl findByShortKeyOrNull(String shortKey) {
        return shortUrlRepository.findByShortKey(shortKey).orElse(null);
    }

    private Page<ShortUrlWithClickCount> toPage(List<ShortUrl> overFetched, PageParam pageParam) {
        boolean hasNext = overFetched.size() > pageParam.size();
        List<ShortUrl> shortUrls = hasNext ? overFetched.subList(0, pageParam.size()) : overFetched;
        Map<Long, Long> clickCounts = findClickCounts(shortUrls);

        List<ShortUrlWithClickCount> content = shortUrls.stream()
            .map(shortUrl -> ShortUrlWithClickCount.of(shortUrl, clickCounts.getOrDefault(shortUrl.id(), 0L)))
            .toList();
        return Page.of(content, hasNext);
    }

    private Map<Long, Long> findClickCounts(List<ShortUrl> shortUrls) {
        List<Long> shortUrlIds = shortUrls.stream().map(ShortUrl::id).toList();
        return clickCountRepository.findAllById(shortUrlIds).stream()
            .collect(Collectors.toMap(ClickCount::shortUrlId, ClickCount::count));
    }

}
