package com.hugo.tinyurl.support.time;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
class SystemClockProvider implements ClockProvider {

    private final Clock clock = Clock.systemDefaultZone();

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

}
