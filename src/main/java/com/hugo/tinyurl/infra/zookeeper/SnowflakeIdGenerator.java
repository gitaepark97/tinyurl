package com.hugo.tinyurl.infra.zookeeper;

import com.hugo.tinyurl.domain.port.IdGenerator;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
class SnowflakeIdGenerator implements IdGenerator {

    private static final long EPOCH = 1767225600000L; // 2026-01-01T00:00:00Z
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("worker-id는 0~" + MAX_WORKER_ID + " 사이여야 합니다 - workerId=" + workerId);
        }
        this.workerId = workerId;
    }

    @Override
    public synchronized long generate() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

}
