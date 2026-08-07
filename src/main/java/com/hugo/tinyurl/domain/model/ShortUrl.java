package com.hugo.tinyurl.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;

public record ShortUrl(
    Long id,
    String shortKey,
    String originalUrl,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {

    private static final Duration EXPIRATION = Duration.ofDays(7);

    public static ShortUrl create(Long id, String shortKey, String originalUrl, LocalDateTime now) {
        return new ShortUrl(id, shortKey, originalUrl, now.plus(EXPIRATION), now);
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

}
