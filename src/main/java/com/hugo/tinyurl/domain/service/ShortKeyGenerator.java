package com.hugo.tinyurl.domain.service;

import com.hugo.tinyurl.domain.repository.Counter;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sqids.Sqids;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
class ShortKeyGenerator {

    private static final int KEY_LENGTH = 8;
    private static final Sqids SQIDS = Sqids.builder().minLength(KEY_LENGTH).build();

    private final Counter counter;

    String generate() {
        long counterValue = counter.next();
        String key = SQIDS.encode(List.of(counterValue));
        if (key.length() != KEY_LENGTH) {
            log.error("short key 발급 가능 공간을 초과했습니다 - counterValue={}", counterValue);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return key;
    }

}
