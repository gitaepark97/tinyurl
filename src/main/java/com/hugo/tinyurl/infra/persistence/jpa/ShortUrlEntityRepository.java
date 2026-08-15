package com.hugo.tinyurl.infra.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlEntityRepository extends JpaRepository<ShortUrlJpaEntity, Long> {

    Optional<ShortUrlJpaEntity> findByShortKey(String shortKey);

    List<ShortUrlJpaEntity> findByIdLessThanOrderByIdDesc(Long id, Pageable pageable);

    List<ShortUrlJpaEntity> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long id, Pageable pageable);

    List<ShortUrlJpaEntity> findByExpiresAtBeforeOrderByIdAsc(LocalDateTime dateTime, Pageable pageable);

}
