package com.hugo.tinyurl.infra.persistence.jpa;

import com.hugo.tinyurl.domain.model.ShortUrl;
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
@Table(name = "short_url")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShortUrlJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_key", nullable = false)
    private String shortKey;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ShortUrlJpaEntity(Long id, String shortKey, String originalUrl, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.id = id;
        this.shortKey = shortKey;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static ShortUrlJpaEntity from(ShortUrl shortUrl) {
        return new ShortUrlJpaEntity(
            shortUrl.id(), shortUrl.shortKey(), shortUrl.originalUrl(), shortUrl.expiresAt(), shortUrl.createdAt());
    }

    public ShortUrl toDomain() {
        return new ShortUrl(id, shortKey, originalUrl, expiresAt, createdAt);
    }

}
