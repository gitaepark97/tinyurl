package com.hugo.tinyurl.web.controller.v1.response;

import com.hugo.tinyurl.domain.model.ClickEvent;
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
