package com.hugo.tinyurl.common.infra.coordination;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.port.DistributedLock;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ZookeeperDistributedLock implements DistributedLock {

    private static final String LOCK_PATH_PREFIX = "/tinyurl/locks/";

    private final CuratorFramework lockCuratorFramework;

    @Override
    public boolean tryRun(String key, Runnable action) {
        InterProcessMutex mutex = new InterProcessMutex(lockCuratorFramework, LOCK_PATH_PREFIX + key);
        boolean acquired;
        try {
            // 블로킹 대기 없이 즉시 판단한다 - 다른 인스턴스가 실행 중이면 이번 스케줄은 그냥 건너뛴다.
            acquired = mutex.acquire(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
        if (!acquired) {
            return false;
        }
        try {
            action.run();
        } finally {
            release(mutex, key);
        }
        return true;
    }

    private void release(InterProcessMutex mutex, String key) {
        try {
            mutex.release();
        } catch (Exception e) {
            log.warn("분산 락 해제 실패 - key={}", key, e);
        }
    }

}
