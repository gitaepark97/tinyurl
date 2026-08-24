package com.hugo.tinyurl.shorturl.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ShortUrlTest {

    @Test
    void createDefaultsToAnonymousWithSevenDayExpiration() {
        LocalDateTime now = LocalDateTime.now();

        ShortUrl shortUrl = ShortUrl.create(1L, "abc12345", "https://example.com", now);

        assertThat(shortUrl.memberId()).isNull();
        assertThat(shortUrl.expiresAt()).isEqualTo(now.plusDays(7));
    }

    @Test
    void createForMemberUsesGivenExpiresAtWithinOneMonth() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(20);

        ShortUrl shortUrl = ShortUrl.createForMember(1L, "abc12345", "https://example.com", 10L, expiresAt, now);

        assertThat(shortUrl.memberId()).isEqualTo(10L);
        assertThat(shortUrl.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void createForMemberDefaultsToSevenDaysWhenExpiresAtOmitted() {
        LocalDateTime now = LocalDateTime.now();

        ShortUrl shortUrl = ShortUrl.createForMember(1L, "abc12345", "https://example.com", 10L, null, now);

        assertThat(shortUrl.expiresAt()).isEqualTo(now.plusDays(7));
    }

    @Test
    void createForMemberRejectsExpiresAtBeyondOneMonth() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tooFar = now.plusDays(31);

        assertThatThrownBy(() -> ShortUrl.createForMember(1L, "abc12345", "https://example.com", 10L, tooFar, now))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void isOwnedByReturnsTrueOnlyForMatchingMemberId() {
        ShortUrl shortUrl = ShortUrl.createForMember(1L, "abc12345", "https://example.com", 10L, null, LocalDateTime.now());

        assertThat(shortUrl.isOwnedBy(10L)).isTrue();
        assertThat(shortUrl.isOwnedBy(99L)).isFalse();
        assertThat(shortUrl.isOwnedBy(null)).isFalse();
    }

    @Test
    void anonymousShortUrlIsOwnedByNobody() {
        ShortUrl shortUrl = ShortUrl.create(1L, "abc12345", "https://example.com", LocalDateTime.now());

        assertThat(shortUrl.isOwnedBy(10L)).isFalse();
    }

}
