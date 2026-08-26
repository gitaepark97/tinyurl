package com.hugo.tinyurl.common.infra.coordination;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.port.Counter;
import lombok.RequiredArgsConstructor;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ZookeeperCounter implements Counter {

    private final DistributedAtomicLong shortKeyCounter;

    @Override
    public long next() {
        AtomicValue<Long> result;
        try {
            result = shortKeyCounter.increment();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
        if (!result.succeeded()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return result.postValue();
    }

}
