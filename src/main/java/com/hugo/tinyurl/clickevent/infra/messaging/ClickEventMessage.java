package com.hugo.tinyurl.clickevent.infra.messaging;

record ClickEventMessage(
    Long shortUrlId,
    String ipAddress,
    String userAgent,
    String referer
) {
}
