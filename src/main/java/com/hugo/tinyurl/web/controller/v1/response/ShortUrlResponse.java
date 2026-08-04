package com.hugo.tinyurl.web.controller.v1.response;

import com.hugo.tinyurl.domain.dto.ShortUrlWithClickCount;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import java.time.LocalDateTime;

public record ShortUrlResponse(
    Long id,
    String shortKey,
    String shortUrl,
    String originalUrl,
    long clickCount,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {

    public static ShortUrlResponse from(ShortUrlWithClickCount view, String baseUrl) {
        ShortUrl shortUrl = view.shortUrl();
        return new ShortUrlResponse(
            shortUrl.getId(),
            shortUrl.getShortKey(),
            baseUrl + "/" + shortUrl.getShortKey(),
            shortUrl.getOriginalUrl(),
            view.clickCount(),
            shortUrl.getExpiresAt(),
            shortUrl.getCreatedAt()
        );
    }

}
