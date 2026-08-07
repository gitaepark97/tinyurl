package com.hugo.tinyurl.domain.port;

import com.hugo.tinyurl.domain.model.ShortUrl;
import java.util.Optional;
import java.util.function.Function;

public interface ShortUrlCacheRepository {

    Optional<ShortUrl> findByShortKey(String shortKey, Function<String, ShortUrl> loader);

    void evict(String shortKey);

}
