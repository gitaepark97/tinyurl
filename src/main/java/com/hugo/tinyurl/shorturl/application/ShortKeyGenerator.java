package com.hugo.tinyurl.shorturl.application;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.port.Counter;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sqids.Sqids;

@Observed
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
