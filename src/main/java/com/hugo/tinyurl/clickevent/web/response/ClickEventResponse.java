package com.hugo.tinyurl.clickevent.web.response;

import com.hugo.tinyurl.clickevent.model.ClickEvent;
import java.time.LocalDateTime;

public record ClickEventResponse(
    Long id,
    String ipAddress,
    String userAgent,
    String referer,
    LocalDateTime clickedAt
) {

    public static ClickEventResponse from(ClickEvent clickEvent) {
        return new ClickEventResponse(
            clickEvent.id(),
            clickEvent.ipAddress(),
            clickEvent.userAgent(),
            clickEvent.referer(),
            clickEvent.clickedAt()
        );
    }

}
