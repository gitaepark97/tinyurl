package com.hugo.tinyurl.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ShortUrlCacheRepositoryTest {

    private final ShortUrlCacheRepository shortUrlCacheRepository = new ShortUrlCacheRepository(100_000, 10);

    @Test
    void cachesLoaderResultForSameKey() {
        ShortUrl shortUrl = new ShortUrl("abc12345", "https://example.com", LocalDateTime.now().plusDays(7));
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
        assertThat(second).contains(shortUrl);
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
        ShortUrl shortUrl = new ShortUrl("abc12345", "https://example.com", LocalDateTime.now().plusDays(7));
        shortUrlCacheRepository.findByShortKey("abc12345", key -> shortUrl);

        shortUrlCacheRepository.evict("abc12345");

        AtomicInteger loadCount = new AtomicInteger();
        shortUrlCacheRepository.findByShortKey("abc12345", key -> {
            loadCount.incrementAndGet();
            return null;
        });

        assertThat(loadCount).hasValue(1);
    }

}
