package com.hugo.tinyurl.domain.port;

import java.time.LocalDateTime;

public interface ClockProvider {

    LocalDateTime now();

}
