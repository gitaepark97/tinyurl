package com.hugo.tinyurl.shorturl.model;

import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;

import java.time.Duration;
import java.time.LocalDateTime;

public record ShortUrl(
    Long id,
    String shortKey,
    String originalUrl,
    Long memberId,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {

    private static final Duration DEFAULT_EXPIRATION = Duration.ofDays(7);
    private static final Duration MAX_MEMBER_EXPIRATION = Duration.ofDays(30);

    public static ShortUrl create(Long id, String shortKey, String originalUrl, LocalDateTime now) {
        return new ShortUrl(id, shortKey, originalUrl, null, now.plus(DEFAULT_EXPIRATION), now);
    }

    public static ShortUrl createForMember(
        Long id,
        String shortKey,
        String originalUrl,
        Long memberId,
        LocalDateTime expiresAt,
        LocalDateTime now
    ) {
        LocalDateTime resolvedExpiresAt = expiresAt != null ? expiresAt : now.plus(DEFAULT_EXPIRATION);
        if (resolvedExpiresAt.isAfter(now.plus(MAX_MEMBER_EXPIRATION))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return new ShortUrl(id, shortKey, originalUrl, memberId, resolvedExpiresAt, now);
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean isOwnedBy(Long memberId) {
        return memberId != null && memberId.equals(this.memberId);
    }

}
