package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ClockProvider;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ShortUrlManager {

    private final ShortUrlRepository shortUrlRepository;
    private final ClockProvider clockProvider;
    private final ShortKeyGenerator shortKeyGenerator;

    ShortUrl create(String originalUrl) {
        String shortKey = shortKeyGenerator.generate();
        ShortUrl shortUrl = ShortUrl.create(shortKey, originalUrl, clockProvider.now());
        try {
            return shortUrlRepository.save(shortUrl);
        } catch (DataIntegrityViolationException e) {
            log.error("short_key 충돌 발생 - shortKey={}", shortKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

}
