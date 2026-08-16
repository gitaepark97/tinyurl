package com.hugo.tinyurl.common.port;

public interface DistributedLock {

    // 락을 즉시(논블로킹) 획득할 수 있으면 action을 실행하고 true, 이미 다른 인스턴스가 점유 중이면 실행하지 않고 false를 반환한다.
    boolean tryRun(String key, Runnable action);

}
