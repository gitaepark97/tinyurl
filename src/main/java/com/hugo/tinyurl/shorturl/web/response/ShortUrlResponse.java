package com.hugo.tinyurl.shorturl.web.response;

import com.hugo.tinyurl.shorturl.model.ShortUrl;
import com.hugo.tinyurl.shorturl.model.ShortUrlWithClickCount;
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
            shortUrl.id(),
            shortUrl.shortKey(),
            baseUrl + "/" + shortUrl.shortKey(),
            shortUrl.originalUrl(),
            view.clickCount(),
            shortUrl.expiresAt(),
            shortUrl.createdAt()
        );
    }

}
