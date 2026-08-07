package com.hugo.tinyurl.infra.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ClickCountEntityRepository extends JpaRepository<ClickCountJpaEntity, Long> {

    @Transactional
    @Modifying
    @Query(
        value = "INSERT INTO click_count (short_url_id, count) VALUES (:shortUrlId, 1) "
            + "ON DUPLICATE KEY UPDATE count = count + 1",
        nativeQuery = true
    )
    void increment(@Param("shortUrlId") Long shortUrlId);

}
