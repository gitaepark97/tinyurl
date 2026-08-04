package com.hugo.tinyurl.domain.repository;

import com.hugo.tinyurl.domain.entity.ClickEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    List<ClickEvent> findByShortUrlIdAndIdLessThanOrderByIdDesc(Long shortUrlId, Long id, Pageable pageable);

}
