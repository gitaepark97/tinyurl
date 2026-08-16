package com.hugo.tinyurl.common.port;

import java.time.LocalDateTime;

public interface ClockProvider {

    LocalDateTime now();

}
