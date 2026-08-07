package com.hugo.tinyurl.infra.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlEntityRepository extends JpaRepository<ShortUrlJpaEntity, Long> {

    Optional<ShortUrlJpaEntity> findByShortKey(String shortKey);

    List<ShortUrlJpaEntity> findByIdLessThanOrderByIdDesc(Long id, Pageable pageable);

}
