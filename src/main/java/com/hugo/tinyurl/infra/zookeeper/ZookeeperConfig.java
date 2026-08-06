package com.hugo.tinyurl.infra.zookeeper;

import java.util.concurrent.TimeUnit;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.framework.recipes.atomic.PromotedToLock;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ZookeeperConfig {

    private static final String SHORT_KEY_COUNTER_PATH = "/tinyurl/short-key-counter";
    private static final String SHORT_KEY_COUNTER_LOCK_PATH = "/tinyurl/short-key-counter-lock";
    private static final String WORKER_ID_PATH_PREFIX = "/tinyurl/id-generator/workers/worker-";
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final long MAX_WORKER_ID = 1023L; // IdGenerator 구현체의 worker-id 비트폭(10bit)과 맞춤

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
        String createdPath = curatorFramework.create()
            .creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
            .forPath(WORKER_ID_PATH_PREFIX);
        long sequence = Long.parseLong(createdPath.substring(createdPath.lastIndexOf('-') + 1));
        return sequence % (MAX_WORKER_ID + 1);
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
