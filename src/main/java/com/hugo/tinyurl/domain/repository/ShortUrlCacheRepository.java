package com.hugo.tinyurl.domain.repository;

import com.hugo.tinyurl.domain.entity.ShortUrl;
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
public class ShortUrlCacheRepository {

    private static final String KEY_PREFIX = "short-url:";

    private final RedisTemplate<String, ShortUrl> redisTemplate;
    private final Duration ttl;

    public ShortUrlCacheRepository(
        RedisTemplate<String, ShortUrl> redisTemplate,
        @Value("${app.cache.short-url.expire-after-access-minutes}") long expireAfterAccessMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(expireAfterAccessMinutes);
    }

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

    public void evict(String shortKey) {
        try {
            redisTemplate.delete(key(shortKey));
        } catch (DataAccessException e) {
            log.warn("Redis evict 실패 - shortKey={}", shortKey, e);
        }
    }

    private ShortUrl getFromCache(String key) {
        try {
            return redisTemplate.opsForValue().getAndExpire(key, ttl);
        } catch (DataAccessException e) {
            log.warn("Redis 조회 실패 - key={}", key, e);
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
