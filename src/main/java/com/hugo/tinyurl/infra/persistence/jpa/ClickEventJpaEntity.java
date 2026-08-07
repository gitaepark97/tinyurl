package com.hugo.tinyurl.infra.persistence.jpa;

import com.hugo.tinyurl.domain.model.ClickEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "click_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClickEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_url_id", nullable = false)
    private Long shortUrlId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "referer")
    private String referer;

    @Column(name = "clicked_at", nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    public ClickEventJpaEntity(Long id, Long shortUrlId, String ipAddress, String userAgent, String referer, LocalDateTime clickedAt) {
        this.id = id;
        this.shortUrlId = shortUrlId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
        this.clickedAt = clickedAt;
    }

    public static ClickEventJpaEntity from(ClickEvent clickEvent) {
        return new ClickEventJpaEntity(
            clickEvent.id(), clickEvent.shortUrlId(), clickEvent.ipAddress(), clickEvent.userAgent(), clickEvent.referer(),
            clickEvent.clickedAt());
    }

    public ClickEvent toDomain() {
        return new ClickEvent(id, shortUrlId, ipAddress, userAgent, referer, clickedAt);
    }

}
