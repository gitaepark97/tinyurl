package com.hugo.tinyurl.clickevent.infra.persistence.jpa;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ClickEventEntityRepository extends JpaRepository<ClickEventJpaEntity, Long> {

    boolean existsByDeliveryKey(String deliveryKey);

    List<ClickEventJpaEntity> findByShortUrlIdAndIdLessThanOrderByIdDesc(Long shortUrlId, Long id, Pageable pageable);

    List<ClickEventJpaEntity> findByShortUrlIdInAndIdGreaterThanOrderByIdAsc(List<Long> shortUrlIds, Long id, Pageable pageable);

    @Transactional
    @Modifying
    @Query("delete from ClickEventJpaEntity c where c.shortUrlId in :shortUrlIds")
    void deleteAllByShortUrlIdIn(@Param("shortUrlIds") List<Long> shortUrlIds);

}
