package com.hugo.tinyurl.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.entity.ShortUrl;
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
class ShortUrlCacheRepositoryTest {

    @Autowired
    ShortUrlCacheRepository shortUrlCacheRepository;

    @AfterEach
    void cleanUpCache() {
        shortUrlCacheRepository.evict("abc12345");
        shortUrlCacheRepository.evict("nope0000");
        shortUrlCacheRepository.evict("evt12345");
    }

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
        ShortUrl shortUrl = new ShortUrl("evt12345", "https://example.com", LocalDateTime.now().plusDays(7));
        shortUrlCacheRepository.findByShortKey("evt12345", key -> shortUrl);

        shortUrlCacheRepository.evict("evt12345");

        AtomicInteger loadCount = new AtomicInteger();
        shortUrlCacheRepository.findByShortKey("evt12345", key -> {
            loadCount.incrementAndGet();
            return null;
        });

        assertThat(loadCount).hasValue(1);
    }

}
