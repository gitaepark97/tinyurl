package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.time.ClockProvider;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ShortUrlManager {

    private static final Duration EXPIRATION = Duration.ofDays(7);

    private final ShortUrlRepository shortUrlRepository;
    private final ClockProvider clockProvider;
    private final ShortKeyGenerator shortKeyGenerator;

    ShortUrl create(String originalUrl) {
        String shortKey = shortKeyGenerator.generate();
        try {
            return shortUrlRepository.save(new ShortUrl(shortKey, originalUrl, clockProvider.now().plus(EXPIRATION)));
        } catch (DataIntegrityViolationException e) {
            log.error("short_key 충돌 발생 - shortKey={}", shortKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

}
