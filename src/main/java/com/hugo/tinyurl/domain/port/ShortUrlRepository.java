package com.hugo.tinyurl.domain.port;

import com.hugo.tinyurl.domain.model.ShortUrl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShortUrlRepository {

    ShortUrl save(ShortUrl shortUrl);

    Optional<ShortUrl> findById(Long id);

    Optional<ShortUrl> findByShortKey(String shortKey);

    List<ShortUrl> findByIdLessThanOrderByIdDesc(Long id, int limit);

    List<ShortUrl> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long id, int limit);

    List<ShortUrl> findByExpiresAtBeforeOrderByIdAsc(LocalDateTime dateTime, int limit);

    void deleteById(Long id);

    void deleteAllById(Iterable<Long> ids);

}
