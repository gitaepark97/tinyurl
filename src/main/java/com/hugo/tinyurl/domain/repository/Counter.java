package com.hugo.tinyurl.domain.repository;

import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.springframework.stereotype.Component;

@Component
public class Counter {

    private final DistributedAtomicLong shortKeyCounter;

    public Counter(DistributedAtomicLong shortKeyCounter) {
        this.shortKeyCounter = shortKeyCounter;
    }

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
