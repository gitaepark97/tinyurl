package com.hugo.tinyurl.domain.repository;

import com.hugo.tinyurl.domain.entity.ClickEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    List<ClickEvent> findByShortUrlId(Long shortUrlId);

}
