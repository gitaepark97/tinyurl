package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ShortUrlManagerTest {

    @Autowired
    ShortUrlRepository shortUrlRepository;

    @Autowired
    ShortUrlManager shortUrlManager;

    private final List<Long> createdShortUrlIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        shortUrlRepository.deleteAllById(createdShortUrlIds);
        createdShortUrlIds.clear();
    }

    @Test
    void createsShortUrlWithSevenDayExpiration() {
        LocalDateTime beforeCreate = LocalDateTime.now();

        ShortUrl shortUrl = ShortUrlTestSupport.create(shortUrlManager, "https://example.com", createdShortUrlIds);

        LocalDateTime afterCreate = LocalDateTime.now();
        assertThat(shortUrl.getShortKey()).hasSize(8);
        assertThat(shortUrl.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(shortUrl.isExpired(LocalDateTime.now())).isFalse();
        assertThat(shortUrl.getExpiresAt()).isBetween(beforeCreate.plusDays(7), afterCreate.plusDays(7));
    }

    @Test
    void issuesNewShortKeyForSameOriginalUrl() {
        ShortUrl first = ShortUrlTestSupport.create(shortUrlManager, "https://example.com", createdShortUrlIds);
        ShortUrl second = ShortUrlTestSupport.create(shortUrlManager, "https://example.com", createdShortUrlIds);

        assertThat(first.getShortKey()).isNotEqualTo(second.getShortKey());
    }

}
