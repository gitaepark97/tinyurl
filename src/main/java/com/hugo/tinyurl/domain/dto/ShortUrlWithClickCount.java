package com.hugo.tinyurl.domain.dto;

import com.hugo.tinyurl.domain.entity.ShortUrl;

public record ShortUrlWithClickCount(
    ShortUrl shortUrl,
    long clickCount
) {

    public static ShortUrlWithClickCount of(ShortUrl shortUrl, long clickCount) {
        return new ShortUrlWithClickCount(shortUrl, clickCount);
    }

}
