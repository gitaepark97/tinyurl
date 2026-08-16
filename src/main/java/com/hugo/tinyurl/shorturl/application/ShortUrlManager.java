package com.hugo.tinyurl.shorturl.application;

import com.hugo.tinyurl.common.port.ClockProvider;
import com.hugo.tinyurl.common.port.IdGenerator;
import com.hugo.tinyurl.shorturl.model.ShortUrl;
import com.hugo.tinyurl.shorturl.port.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Observed
@Slf4j
@Component
@RequiredArgsConstructor
class ShortUrlManager {

    private final ShortUrlRepository shortUrlRepository;
    private final ClockProvider clockProvider;
    private final IdGenerator idGenerator;
    private final ShortKeyGenerator shortKeyGenerator;

    ShortUrl create(Long memberId, String originalUrl, String customAlias, LocalDateTime expiresAt) {
        if (memberId == null) {
            if (customAlias != null || expiresAt != null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            String shortKey = shortKeyGenerator.generate();
            ShortUrl shortUrl = ShortUrl.create(idGenerator.generate(), shortKey, originalUrl, clockProvider.now());
            return save(shortUrl, false);
        }
        String shortKey = customAlias != null ? customAlias : shortKeyGenerator.generate();
        ShortUrl shortUrl = ShortUrl.createForMember(
            idGenerator.generate(), shortKey, originalUrl, memberId, expiresAt, clockProvider.now());
        return save(shortUrl, customAlias != null);
    }

    private ShortUrl save(ShortUrl shortUrl, boolean isUserProvidedKey) {
        try {
            return shortUrlRepository.save(shortUrl);
        } catch (DataIntegrityViolationException e) {
            if (isUserProvidedKey) {
                // 사용자가 지정한 값의 충돌은 예상 가능한 입력 오류 — 재시도하지 않고 즉시 409
                throw new BusinessException(ErrorCode.CONFLICT, e);
            }
            // short_key 또는 PK(id) unique 제약 위반 중 하나이며, 이 예외만으로는 구분할 수 없다.
            log.error("ShortUrl 저장 실패 - id={}, shortKey={}", shortUrl.id(), shortUrl.shortKey(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

}
