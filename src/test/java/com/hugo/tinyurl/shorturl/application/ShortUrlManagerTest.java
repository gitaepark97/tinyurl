package com.hugo.tinyurl.shorturl.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.shorturl.model.ShortUrl;
import com.hugo.tinyurl.shorturl.port.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
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
        assertThat(shortUrl.shortKey()).hasSize(8);
        assertThat(shortUrl.originalUrl()).isEqualTo("https://example.com");
        assertThat(shortUrl.isExpired(LocalDateTime.now())).isFalse();
        assertThat(shortUrl.expiresAt()).isBetween(beforeCreate.plusDays(7), afterCreate.plusDays(7));
    }

    @Test
    void issuesNewShortKeyForSameOriginalUrl() {
        ShortUrl first = ShortUrlTestSupport.create(shortUrlManager, "https://example.com", createdShortUrlIds);
        ShortUrl second = ShortUrlTestSupport.create(shortUrlManager, "https://example.com", createdShortUrlIds);

        assertThat(first.shortKey()).isNotEqualTo(second.shortKey());
    }

    @Test
    void createsForMemberWithCustomAliasAndExpiresAt() {
        String customAlias = "cust" + (System.nanoTime() % 10000);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(10).withNano(0);

        ShortUrl shortUrl = shortUrlManager.create(1L, "https://example.com", customAlias, expiresAt);
        createdShortUrlIds.add(shortUrl.id());

        assertThat(shortUrl.shortKey()).isEqualTo(customAlias);
        assertThat(shortUrl.memberId()).isEqualTo(1L);
        assertThat(shortUrl.expiresAt()).isEqualTo(expiresAt);
        assertThat(shortUrl.isOwnedBy(1L)).isTrue();
    }

    @Test
    void createsForMemberWithGeneratedKeyAndDefaultExpirationWhenNotSpecified() {
        LocalDateTime beforeCreate = LocalDateTime.now();

        ShortUrl shortUrl = shortUrlManager.create(1L, "https://example.com", null, null);
        createdShortUrlIds.add(shortUrl.id());

        LocalDateTime afterCreate = LocalDateTime.now();
        assertThat(shortUrl.shortKey()).hasSize(8);
        assertThat(shortUrl.expiresAt()).isBetween(beforeCreate.plusDays(7), afterCreate.plusDays(7));
    }

    @Test
    void rejectsDuplicateCustomAliasWithConflict() {
        String customAlias = "dup" + (System.nanoTime() % 10000);
        ShortUrl first = shortUrlManager.create(1L, "https://example.com", customAlias, null);
        createdShortUrlIds.add(first.id());

        assertThatThrownBy(() -> shortUrlManager.create(2L, "https://example.com", customAlias, null))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void rejectsExpiresAtBeyondOneMonth() {
        LocalDateTime tooFar = LocalDateTime.now().plusDays(31);

        assertThatThrownBy(() -> shortUrlManager.create(1L, "https://example.com", null, tooFar))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsCustomAliasWithoutMemberId() {
        assertThatThrownBy(() -> shortUrlManager.create(null, "https://example.com", "custom01", null))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void rejectsExpiresAtWithoutMemberId() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);

        assertThatThrownBy(() -> shortUrlManager.create(null, "https://example.com", null, expiresAt))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.FORBIDDEN);
    }

}
