package com.hugo.tinyurl.shorturl.infra.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
class RateLimitConfig {

    // 만료 버퍼일 뿐이라 여러 rate limit 사용처가 이 값 하나를 공유해도 된다 - 실제 리필 시점은 버킷 자신의 상태로 계산된다.
    @Lazy
    @Bean
    ProxyManager<byte[]> rateLimitProxyManager(
        LettuceConnectionFactory connectionFactory,
        @Value("${app.rate-limit.anonymous-url-creation.refill-duration-seconds}") long refillDurationSeconds
    ) {
        RedisClient redisClient = (RedisClient) connectionFactory.getRequiredNativeClient();
        return Bucket4jLettuce.casBasedBuilder(redisClient)
            .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                Duration.ofSeconds(refillDurationSeconds)))
            .build();
    }

}
