package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.dto.ShortUrlWithClickCount;
import com.hugo.tinyurl.domain.entity.ClickCount;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ClickCountRepository;
import com.hugo.tinyurl.domain.repository.ShortUrlCacheRepository;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import com.hugo.tinyurl.support.time.ClockProvider;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
            pageParam.cursorOrInitial(), PageRequest.of(0, pageParam.size() + 1));

        boolean hasNext = overFetched.size() > pageParam.size();
        List<ShortUrl> shortUrls = hasNext ? overFetched.subList(0, pageParam.size()) : overFetched;
        Map<Long, Long> clickCounts = findClickCounts(shortUrls);

        List<ShortUrlWithClickCount> content = shortUrls.stream()
            .map(shortUrl -> ShortUrlWithClickCount.of(shortUrl, clickCounts.getOrDefault(shortUrl.getId(), 0L)))
            .toList();
        return Page.of(content, hasNext);
    }

    @Transactional(readOnly = true)
    ShortUrlWithClickCount get(Long id) {
        ShortUrl shortUrl = shortUrlRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        long clickCount = clickCountRepository.findById(shortUrl.getId())
            .map(ClickCount::getCount)
            .orElse(0L);

        return ShortUrlWithClickCount.of(shortUrl, clickCount);
    }

    private ShortUrl findValidByShortKey(String shortKey) {
        return shortUrlRepository.findByShortKey(shortKey)
            .filter(url -> !url.isExpired(clockProvider.now()))
            .orElse(null);
    }

    private Map<Long, Long> findClickCounts(List<ShortUrl> shortUrls) {
        List<Long> shortUrlIds = shortUrls.stream().map(ShortUrl::getId).toList();
        return clickCountRepository.findAllById(shortUrlIds).stream()
            .collect(Collectors.toMap(ClickCount::getShortUrlId, ClickCount::getCount));
    }

}
