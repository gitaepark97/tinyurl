package com.hugo.tinyurl.clickevent.port;

public interface ClickEventPublisher {

    void publish(Long shortUrlId, String ipAddress, String userAgent, String referer);

}
