package com.hugo.tinyurl.infra.coordination;

import java.util.concurrent.TimeUnit;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.framework.recipes.atomic.PromotedToLock;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ZookeeperConfig {

    private static final String SHORT_KEY_COUNTER_PATH = "/tinyurl/short-key-counter";
    private static final String SHORT_KEY_COUNTER_LOCK_PATH = "/tinyurl/short-key-counter-lock";
    private static final String WORKER_ID_PATH_PREFIX = "/tinyurl/id-generator/workers/worker-";
    private static final int CONNECT_TIMEOUT_SECONDS = 10;

    private static final RetryPolicy RETRY_POLICY = new RetryNTimes(3, 100);
    // 뮤텍스로 승격된 뒤에는 포기하지 않고 기다리는 게 맞으므로, 연결/낙관적 CAS용 RETRY_POLICY보다
    // 훨씬 여유 있게(최대 10회, 최대 1초 간격 지수 백오프) 잡는다.
    private static final RetryPolicy LOCK_RETRY_POLICY = new BoundedExponentialBackoffRetry(50, 1000, 10);

    @Bean(destroyMethod = "close")
    CuratorFramework curatorFramework(@Value("${app.zookeeper.connect-string}") String connectString) {
        CuratorFramework client = CuratorFrameworkFactory.newClient(connectString, RETRY_POLICY);
        client.start();
        awaitConnection(client, connectString);
        return client;
    }

    // 락 recipe(InterProcessMutex 등)는 CuratorFramework 생성 시점의 재시도 정책을 그대로 쓰므로,
    // 연결/CAS용보다 여유 있는 LOCK_RETRY_POLICY를 쓰려면 별도 커넥션이 필요하다.
    @Bean(destroyMethod = "close")
    CuratorFramework lockCuratorFramework(@Value("${app.zookeeper.connect-string}") String connectString) {
        CuratorFramework client = CuratorFrameworkFactory.newClient(connectString, LOCK_RETRY_POLICY);
        client.start();
        awaitConnection(client, connectString);
        return client;
    }

    @Bean
    DistributedAtomicLong shortKeyCounter(CuratorFramework curatorFramework) {
        PromotedToLock promotedToLock = PromotedToLock.builder()
            .lockPath(SHORT_KEY_COUNTER_LOCK_PATH)
            .retryPolicy(LOCK_RETRY_POLICY)
            .build();
        return new DistributedAtomicLong(curatorFramework, SHORT_KEY_COUNTER_PATH, RETRY_POLICY, promotedToLock);
    }

    @Bean
    long workerId(CuratorFramework curatorFramework) throws Exception {
        // 0~MAX_WORKER_ID 슬롯 중 비어있는 첫 번째를 EPHEMERAL 노드로 선점한다.
        // 세션 종료(정상 종료/크래시) 시 노드가 자동 삭제되어 슬롯이 즉시 재사용되므로,
        // 계속 증가하기만 하는 시퀀스 번호를 쓰는 방식과 달리 재시작이 누적돼도 worker-id가 wrap되지 않는다.
        for (long id = 0; id <= SnowflakeIdGenerator.MAX_WORKER_ID; id++) {
            try {
                curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(WORKER_ID_PATH_PREFIX + id);
                return id;
            } catch (KeeperException.NodeExistsException e) {
                // 다른 인스턴스가 이미 점유한 슬롯 -> 다음 슬롯 시도
            }
        }
        throw new IllegalStateException("사용 가능한 worker-id가 없습니다 - max=" + SnowflakeIdGenerator.MAX_WORKER_ID);
    }

    private void awaitConnection(CuratorFramework client, String connectString) {
        try {
            if (!client.blockUntilConnected(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("ZooKeeper 연결에 실패했습니다 - connectString=" + connectString);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ZooKeeper 연결 대기 중 인터럽트되었습니다 - connectString=" + connectString, e);
        }
    }

}
