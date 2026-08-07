package com.hugo.tinyurl.infra.persistence.jpa;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventEntityRepository extends JpaRepository<ClickEventJpaEntity, Long> {

    List<ClickEventJpaEntity> findByShortUrlIdAndIdLessThanOrderByIdDesc(Long shortUrlId, Long id, Pageable pageable);

}
