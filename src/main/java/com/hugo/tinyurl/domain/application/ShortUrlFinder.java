package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ClickCount;
import com.hugo.tinyurl.domain.model.Role;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.model.ShortUrlWithClickCount;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.domain.port.ClockProvider;
import com.hugo.tinyurl.domain.port.ShortUrlCacheRepository;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ShortUrlFinder {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickCountRepository clickCountRepository;
    private final ClockProvider clockProvider;
    private final ShortUrlCacheRepository shortUrlCacheRepository;

    @Transactional(readOnly = true)
    ShortUrl find(String shortKey) {
        ShortUrl shortUrl = shortUrlCacheRepository.findByShortKey(shortKey, this::findValidByShortKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (shortUrl.isExpired(clockProvider.now())) {
            shortUrlCacheRepository.evict(shortKey);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

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
        ShortUrl shortUrl = shortUrlRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (requesterRole != Role.ADMIN && !shortUrl.isOwnedBy(requesterMemberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        long clickCount = clickCountRepository.findById(shortUrl.id())
            .map(ClickCount::count)
            .orElse(0L);

        return ShortUrlWithClickCount.of(shortUrl, clickCount);
    }

    private ShortUrl findValidByShortKey(String shortKey) {
        return shortUrlRepository.findByShortKey(shortKey)
            .filter(url -> !url.isExpired(clockProvider.now()))
            .orElse(null);
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
