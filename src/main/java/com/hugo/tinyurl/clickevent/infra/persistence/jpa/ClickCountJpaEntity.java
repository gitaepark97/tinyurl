package com.hugo.tinyurl.clickevent.infra.persistence.jpa;

import com.hugo.tinyurl.clickevent.model.ClickCount;
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
public class ClickCountJpaEntity {

    @Id
    @Column(name = "short_url_id")
    private Long shortUrlId;

    @Column(name = "count", nullable = false)
    private long count;

    public ClickCount toDomain() {
        return new ClickCount(shortUrlId, count);
    }

}
