package com.hugo.tinyurl.shorturl.port;

import com.hugo.tinyurl.shorturl.model.ShortUrl;
import java.util.Optional;
import java.util.function.Function;

public interface ShortUrlCacheRepository {

    Optional<ShortUrl> findByShortKey(String shortKey, Function<String, ShortUrl> loader);

    void evict(String shortKey);

}
