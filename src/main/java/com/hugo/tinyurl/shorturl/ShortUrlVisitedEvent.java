package com.hugo.tinyurl.shorturl;

// 리다이렉트가 일어났다는 사실만 담는다 - clickevent가 이 이벤트를 구독해 클릭 기록을 수행한다.
public record ShortUrlVisitedEvent(Long shortUrlId, String ipAddress, String userAgent, String referer) {
}
