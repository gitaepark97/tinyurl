package com.hugo.tinyurl.shorturl.infra.cache;

import com.hugo.tinyurl.shorturl.model.ShortUrl;
import com.hugo.tinyurl.shorturl.port.ShortUrlCacheRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class RedisShortUrlCacheRepository implements ShortUrlCacheRepository {

    private static final String KEY_PREFIX = "short-url:";

    private final RedisTemplate<String, ShortUrl> redisTemplate;
    private final Duration ttl;
    private final Counter hitCounter;
    private final Counter missCounter;
    private final Counter errorCounter;

    RedisShortUrlCacheRepository(
        RedisTemplate<String, ShortUrl> redisTemplate,
        MeterRegistry meterRegistry,
        @Value("${app.cache.short-url.expire-after-access-minutes}") long expireAfterAccessMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(expireAfterAccessMinutes);
        this.hitCounter = Counter.builder("short_url.cache.access").tag("result", "hit").register(meterRegistry);
        this.missCounter = Counter.builder("short_url.cache.access").tag("result", "miss").register(meterRegistry);
        this.errorCounter = Counter.builder("short_url.cache.access").tag("result", "error").register(meterRegistry);
    }

    @Override
    public Optional<ShortUrl> findByShortKey(String shortKey, Function<String, ShortUrl> loader) {
        String key = key(shortKey);
        ShortUrl cached = getFromCache(key);
        if (cached != null) {
            return Optional.of(cached);
        }

        ShortUrl loaded = loader.apply(shortKey);
        if (loaded != null) {
            putToCache(key, loaded);
        }
        return Optional.ofNullable(loaded);
    }

    @Override
    public void evict(String shortKey) {
        try {
            redisTemplate.delete(key(shortKey));
        } catch (DataAccessException e) {
            log.warn("Redis evict 실패 - shortKey={}", shortKey, e);
        }
    }

    // Redis 조회 실패를 캐시 미스와 같이 세면 Redis 장애가 히트율 저하로만 보여서 대시보드에서 놓치기 쉽다.
    private ShortUrl getFromCache(String key) {
        try {
            ShortUrl cached = redisTemplate.opsForValue().getAndExpire(key, ttl);
            (cached != null ? hitCounter : missCounter).increment();
            return cached;
        } catch (DataAccessException e) {
            log.warn("Redis 조회 실패 - key={}", key, e);
            errorCounter.increment();
            return null;
        }
    }

    private void putToCache(String key, ShortUrl shortUrl) {
        try {
            redisTemplate.opsForValue().set(key, shortUrl, ttl);
        } catch (DataAccessException e) {
            log.warn("Redis 저장 실패 - key={}", key, e);
        }
    }

    private String key(String shortKey) {
        return KEY_PREFIX + shortKey;
    }

}
