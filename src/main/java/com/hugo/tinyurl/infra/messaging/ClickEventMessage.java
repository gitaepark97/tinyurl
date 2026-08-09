package com.hugo.tinyurl.infra.messaging;

record ClickEventMessage(
    Long shortUrlId,
    String ipAddress,
    String userAgent,
    String referer
) {
}
