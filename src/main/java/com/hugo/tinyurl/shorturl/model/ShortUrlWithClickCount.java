package com.hugo.tinyurl.shorturl.model;

public record ShortUrlWithClickCount(
    ShortUrl shortUrl,
    long clickCount
) {

    public static ShortUrlWithClickCount of(ShortUrl shortUrl, long clickCount) {
        return new ShortUrlWithClickCount(shortUrl, clickCount);
    }

}
