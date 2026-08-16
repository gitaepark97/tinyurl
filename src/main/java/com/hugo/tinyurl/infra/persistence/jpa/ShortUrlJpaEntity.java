package com.hugo.tinyurl.infra.persistence.jpa;

import com.hugo.tinyurl.common.infra.persistence.jpa.AppendOnlyJpaEntity;
import com.hugo.tinyurl.domain.model.ShortUrl;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "short_url")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ShortUrlJpaEntity extends AppendOnlyJpaEntity<Long> {

    @Id
    private Long id;

    @Column(name = "short_key", nullable = false)
    private String shortKey;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ShortUrlJpaEntity from(ShortUrl shortUrl) {
        return new ShortUrlJpaEntity(
            shortUrl.id(), shortUrl.shortKey(), shortUrl.originalUrl(), shortUrl.memberId(),
            shortUrl.expiresAt(), shortUrl.createdAt());
    }

    public ShortUrl toDomain() {
        return new ShortUrl(id, shortKey, originalUrl, memberId, expiresAt, createdAt);
    }

}
