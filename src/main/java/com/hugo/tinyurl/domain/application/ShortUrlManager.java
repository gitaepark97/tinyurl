package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ClockProvider;
import com.hugo.tinyurl.domain.port.IdGenerator;
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
    private final IdGenerator idGenerator;
    private final ShortKeyGenerator shortKeyGenerator;

    ShortUrl create(String originalUrl) {
        String shortKey = shortKeyGenerator.generate();
        ShortUrl shortUrl = ShortUrl.create(idGenerator.generate(), shortKey, originalUrl, clockProvider.now());
        try {
            return shortUrlRepository.save(shortUrl);
        } catch (DataIntegrityViolationException e) {
            // short_key 또는 PK(id) unique 제약 위반 중 하나이며, 이 예외만으로는 구분할 수 없다.
            log.error("ShortUrl 저장 실패 - id={}, shortKey={}", shortUrl.id(), shortKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

}
