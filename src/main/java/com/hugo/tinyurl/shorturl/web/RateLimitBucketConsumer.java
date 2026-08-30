package com.hugo.tinyurl.shorturl.web;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TimeoutException;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class RateLimitBucketConsumer {

    private RateLimitBucketConsumer() {
    }

    // rate limit은 보호 장치일 뿐이라 Redis 장애/타임아웃 시엔 fail-open - 다른 예외까지 삼키면 안 돼 넓게 잡지 않는다.
    static boolean tryConsume(ProxyManager<byte[]> proxyManager, String key, BucketConfiguration bucketConfiguration) {
        try {
            Bucket bucket = proxyManager.getProxy(key.getBytes(StandardCharsets.UTF_8), () -> bucketConfiguration);
            return bucket.tryConsume(1);
        } catch (RedisException | TimeoutException e) {
            log.warn("Rate limit 확인 실패 - 요청을 통과시킨다", e);
            return true;
        }
    }

}
