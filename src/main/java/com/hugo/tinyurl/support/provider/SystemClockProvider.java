package com.hugo.tinyurl.support.provider;

import com.hugo.tinyurl.common.port.ClockProvider;
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
