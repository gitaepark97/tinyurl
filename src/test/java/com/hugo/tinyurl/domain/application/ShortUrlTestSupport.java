package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import java.time.LocalDateTime;
import java.util.List;

final class ShortUrlTestSupport {

    private ShortUrlTestSupport() {
    }

    static ShortUrl create(ShortUrlManager shortUrlManager, String originalUrl, List<Long> createdShortUrlIds) {
        ShortUrl shortUrl = shortUrlManager.create(originalUrl);
        createdShortUrlIds.add(shortUrl.id());
        return shortUrl;
    }

    static ShortUrl createExpired(
        ShortUrlRepository shortUrlRepository, ShortKeyGenerator shortKeyGenerator, List<Long> createdShortUrlIds
    ) {
        LocalDateTime now = LocalDateTime.now();
        ShortUrl shortUrl = shortUrlRepository.save(
            new ShortUrl(null, shortKeyGenerator.generate(), "https://example.com", now.minusDays(1), now));
        createdShortUrlIds.add(shortUrl.id());
        return shortUrl;
    }

}
