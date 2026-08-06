package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import java.time.LocalDateTime;
import java.util.List;

final class ShortUrlTestSupport {

    private ShortUrlTestSupport() {
    }

    static ShortUrl create(ShortUrlManager shortUrlManager, String originalUrl, List<Long> createdShortUrlIds) {
        ShortUrl shortUrl = shortUrlManager.create(originalUrl);
        createdShortUrlIds.add(shortUrl.getId());
        return shortUrl;
    }

    static ShortUrl createExpired(
        ShortUrlRepository shortUrlRepository, ShortKeyGenerator shortKeyGenerator, List<Long> createdShortUrlIds
    ) {
        ShortUrl shortUrl = shortUrlRepository.save(
            new ShortUrl(shortKeyGenerator.generate(), "https://example.com", LocalDateTime.now().minusDays(1)));
        createdShortUrlIds.add(shortUrl.getId());
        return shortUrl;
    }

}
