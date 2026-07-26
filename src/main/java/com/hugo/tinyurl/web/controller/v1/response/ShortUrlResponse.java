package com.hugo.tinyurl.web.controller.v1.response;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import java.time.LocalDateTime;

public record ShortUrlResponse(
    String shortKey,
    String shortUrl,
    LocalDateTime expiresAt
) {

    public static ShortUrlResponse from(ShortUrl shortUrl, String baseUrl) {
        return new ShortUrlResponse(shortUrl.getShortKey(), baseUrl + "/" + shortUrl.getShortKey(), shortUrl.getExpiresAt());
    }

}
