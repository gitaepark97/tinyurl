package com.hugo.tinyurl.domain.repository;

import com.hugo.tinyurl.domain.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

}
