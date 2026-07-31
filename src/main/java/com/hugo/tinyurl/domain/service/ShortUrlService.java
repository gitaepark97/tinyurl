package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ShortUrlCacheRepository;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private static final Duration EXPIRATION = Duration.ofDays(7);

    private static final Clock CLOCK = Clock.systemDefaultZone();

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlCacheRepository shortUrlCacheRepository;
    private final ShortKeyGenerator shortKeyGenerator;

    public ShortUrl create(String originalUrl) {
        String shortKey = shortKeyGenerator.generate();
        try {
            return shortUrlRepository.save(new ShortUrl(shortKey, originalUrl, LocalDateTime.now(CLOCK).plus(EXPIRATION)));
        } catch (DataIntegrityViolationException e) {
            log.error("short_key 충돌 발생 - shortKey={}", shortKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortKey) {
        ShortUrl shortUrl = shortUrlCacheRepository.findByShortKey(shortKey, this::findValidByShortKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (shortUrl.isExpired(LocalDateTime.now(CLOCK))) {
            shortUrlCacheRepository.evict(shortKey);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        return shortUrl.getOriginalUrl();
    }

    private ShortUrl findValidByShortKey(String shortKey) {
        return shortUrlRepository.findByShortKey(shortKey)
            .filter(url -> !url.isExpired(LocalDateTime.now(CLOCK)))
            .orElse(null);
    }

}
