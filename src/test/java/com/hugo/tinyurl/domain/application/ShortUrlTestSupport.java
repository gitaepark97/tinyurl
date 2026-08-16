package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.common.port.IdGenerator;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import java.time.LocalDateTime;
import java.util.List;

final class ShortUrlTestSupport {

    private ShortUrlTestSupport() {
    }

    static ShortUrl create(ShortUrlManager shortUrlManager, String originalUrl, List<Long> createdShortUrlIds) {
        ShortUrl shortUrl = shortUrlManager.create(null, originalUrl, null, null);
        createdShortUrlIds.add(shortUrl.id());
        return shortUrl;
    }

    static ShortUrl createExpired(
        ShortUrlRepository shortUrlRepository, ShortKeyGenerator shortKeyGenerator, IdGenerator idGenerator,
        List<Long> createdShortUrlIds
    ) {
        LocalDateTime now = LocalDateTime.now();
        ShortUrl shortUrl = shortUrlRepository.save(
            new ShortUrl(idGenerator.generate(), shortKeyGenerator.generate(), "https://example.com", null, now.minusDays(1), now));
        createdShortUrlIds.add(shortUrl.id());
        return shortUrl;
    }

}
