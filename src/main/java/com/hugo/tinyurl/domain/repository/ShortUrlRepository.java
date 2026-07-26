package com.hugo.tinyurl.domain.repository;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortKey(String shortKey);

    boolean existsByShortKey(String shortKey);

}
