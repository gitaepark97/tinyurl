package com.hugo.tinyurl.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "click_count")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClickCount {

    @Id
    @Column(name = "short_url_id")
    private Long shortUrlId;

    @Column(name = "count", nullable = false)
    private long count;

}
