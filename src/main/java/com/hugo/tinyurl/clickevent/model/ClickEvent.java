package com.hugo.tinyurl.clickevent.model;

import java.time.LocalDateTime;

public record ClickEvent(
    Long id,
    Long shortUrlId,
    String ipAddress,
    String userAgent,
    String referer,
    String deliveryKey,
    LocalDateTime clickedAt
) {

    public static ClickEvent create(
        Long id,
        Long shortUrlId,
        String ipAddress,
        String userAgent,
        String referer,
        String deliveryKey,
        LocalDateTime now
    ) {
        return new ClickEvent(id, shortUrlId, ipAddress, userAgent, referer, deliveryKey, now);
    }

}
