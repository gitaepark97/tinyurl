package com.hugo.tinyurl.common.infra.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SystemClockProviderTest {

    @Test
    void returnsCurrentTime() {
        SystemClockProvider clockProvider = new SystemClockProvider();

        LocalDateTime before = LocalDateTime.now();
        LocalDateTime now = clockProvider.now();
        LocalDateTime after = LocalDateTime.now();

        assertThat(now).isBetween(before, after);
    }

}
