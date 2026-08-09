package com.hugo.tinyurl.domain.port;

public interface ClickEventPublisher {

    void publish(Long shortUrlId, String ipAddress, String userAgent, String referer);

}
