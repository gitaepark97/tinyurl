package com.hugo.tinyurl.domain.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ShortUrlCacheRepository {

    private final Cache<String, ShortUrl> cache;

    public ShortUrlCacheRepository(
        @Value("${app.cache.short-url.maximum-size}") long maximumSize,
        @Value("${app.cache.short-url.expire-after-access-minutes}") long expireAfterAccessMinutes
    ) {
        this.cache = Caffeine.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterAccess(Duration.ofMinutes(expireAfterAccessMinutes))
            .build();
    }

    // 같은 키에 대한 동시 미스는 Caffeine이 하나로 묶어 처리한다 — DB 중복 조회 방지.
    public Optional<ShortUrl> findByShortKey(String shortKey, Function<String, ShortUrl> loader) {
        return Optional.ofNullable(cache.get(shortKey, loader));
    }

    public void evict(String shortKey) {
        cache.invalidate(shortKey);
    }

}
