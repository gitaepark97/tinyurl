package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private static final Duration EXPIRATION = Duration.ofDays(7);
    private static final int MAX_KEY_RETRY = 5;
    private static final Clock CLOCK = Clock.systemDefaultZone();

    private final ShortUrlRepository shortUrlRepository;
    private final ShortKeyGenerator shortKeyGenerator;

    @Transactional
    public ShortUrl create(String originalUrl) {
        String shortKey = generateUniqueShortKey();
        return shortUrlRepository.save(new ShortUrl(shortKey, originalUrl, LocalDateTime.now(CLOCK).plus(EXPIRATION)));
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortKey) {
        ShortUrl shortUrl = shortUrlRepository.findByShortKey(shortKey)
            .filter(url -> !url.isExpired(LocalDateTime.now(CLOCK)))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return shortUrl.getOriginalUrl();
    }

    private String generateUniqueShortKey() {
        for (int i = 0; i < MAX_KEY_RETRY; i++) {
            String candidate = shortKeyGenerator.generate();
            if (!shortUrlRepository.existsByShortKey(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

}
