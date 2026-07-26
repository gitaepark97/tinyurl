package com.hugo.tinyurl.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({TestcontainersConfiguration.class, ShortUrlService.class, ShortKeyGenerator.class})
class ShortUrlServiceTest {

    @Autowired
    ShortUrlService shortUrlService;

    @Test
    void createsShortUrlWithSevenDayExpiration() {
        ShortUrl shortUrl = shortUrlService.create("https://example.com");

        assertThat(shortUrl.getShortKey()).hasSize(8);
        assertThat(shortUrl.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(shortUrl.isExpired(LocalDateTime.now())).isFalse();
    }

    @Test
    void issuesNewShortKeyForSameOriginalUrl() {
        ShortUrl first = shortUrlService.create("https://example.com");
        ShortUrl second = shortUrlService.create("https://example.com");

        assertThat(first.getShortKey()).isNotEqualTo(second.getShortKey());
    }

    @Test
    void returnsOriginalUrlForValidKey() {
        ShortUrl shortUrl = shortUrlService.create("https://example.com");

        assertThat(shortUrlService.getOriginalUrl(shortUrl.getShortKey())).isEqualTo("https://example.com");
    }

    @Test
    void throwsNotFoundForUnknownKey() {
        assertThatThrownBy(() -> shortUrlService.getOriginalUrl("nope0000"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

}
