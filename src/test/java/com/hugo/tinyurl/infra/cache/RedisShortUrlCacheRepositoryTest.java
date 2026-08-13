package com.hugo.tinyurl.infra.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ShortUrlCacheRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class RedisShortUrlCacheRepositoryTest {

    @Autowired
    ShortUrlCacheRepository shortUrlCacheRepository;

    @Autowired
    MeterRegistry meterRegistry;

    @AfterEach
    void cleanUpCache() {
        shortUrlCacheRepository.evict("abc12345");
        shortUrlCacheRepository.evict("nope0000");
        shortUrlCacheRepository.evict("evt12345");
    }

    @Test
    void cachesLoaderResultForSameKey() {
        LocalDateTime now = LocalDateTime.now();
        ShortUrl shortUrl = new ShortUrl(1L, "abc12345", "https://example.com", null, now.plusDays(7), now);
        AtomicInteger loadCount = new AtomicInteger();

        shortUrlCacheRepository.findByShortKey("abc12345", key -> {
            loadCount.incrementAndGet();
            return shortUrl;
        });
        var second = shortUrlCacheRepository.findByShortKey("abc12345", key -> {
            loadCount.incrementAndGet();
            return shortUrl;
        });

        assertThat(loadCount).hasValue(1);
        assertThat(second).isPresent();
        assertThat(second.get()).usingRecursiveComparison().isEqualTo(shortUrl);
    }

    @Test
    void doesNotCacheNullLoaderResult() {
        AtomicInteger loadCount = new AtomicInteger();

        shortUrlCacheRepository.findByShortKey("nope0000", key -> {
            loadCount.incrementAndGet();
            return null;
        });
        var second = shortUrlCacheRepository.findByShortKey("nope0000", key -> {
            loadCount.incrementAndGet();
            return null;
        });

        assertThat(loadCount).hasValue(2);
        assertThat(second).isEmpty();
    }

    @Test
    void returnsEmptyAfterEviction() {
        LocalDateTime now = LocalDateTime.now();
        ShortUrl shortUrl = new ShortUrl(2L, "evt12345", "https://example.com", null, now.plusDays(7), now);
        shortUrlCacheRepository.findByShortKey("evt12345", key -> shortUrl);

        shortUrlCacheRepository.evict("evt12345");

        AtomicInteger loadCount = new AtomicInteger();
        shortUrlCacheRepository.findByShortKey("evt12345", key -> {
            loadCount.incrementAndGet();
            return null;
        });

        assertThat(loadCount).hasValue(1);
    }

    @Test
    void incrementsHitAndMissCounters() {
        double hitBefore = cacheAccessCount("hit");
        double missBefore = cacheAccessCount("miss");
        LocalDateTime now = LocalDateTime.now();
        ShortUrl shortUrl = new ShortUrl(3L, "abc12345", "https://example.com", null, now.plusDays(7), now);

        shortUrlCacheRepository.findByShortKey("abc12345", key -> shortUrl);
        shortUrlCacheRepository.findByShortKey("abc12345", key -> shortUrl);

        assertThat(cacheAccessCount("miss")).isEqualTo(missBefore + 1);
        assertThat(cacheAccessCount("hit")).isEqualTo(hitBefore + 1);
    }

    private double cacheAccessCount(String result) {
        return meterRegistry.get("short_url.cache.access").tag("result", result).counter().count();
    }

}
