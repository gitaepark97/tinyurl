package com.hugo.tinyurl.domain.model;

import java.time.LocalDateTime;

public record ClickEvent(
    Long id,
    Long shortUrlId,
    String ipAddress,
    String userAgent,
    String referer,
    LocalDateTime clickedAt
) {

    public static ClickEvent create(Long shortUrlId, String ipAddress, String userAgent, String referer, LocalDateTime now) {
        return new ClickEvent(null, shortUrlId, ipAddress, userAgent, referer, now);
    }

}
