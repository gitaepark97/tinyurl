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

    // 현재 유일한 사용처(익명 rate limit)의 설정값을 그대로 만료 기준으로 쓴다.
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
