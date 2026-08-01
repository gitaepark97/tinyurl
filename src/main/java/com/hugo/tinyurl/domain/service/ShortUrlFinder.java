package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ShortUrlCacheRepository;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.time.ClockProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ShortUrlFinder {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlCacheRepository shortUrlCacheRepository;
    private final ClockProvider clockProvider;

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

    private ShortUrl findValidByShortKey(String shortKey) {
        return shortUrlRepository.findByShortKey(shortKey)
            .filter(url -> !url.isExpired(clockProvider.now()))
            .orElse(null);
    }

}
