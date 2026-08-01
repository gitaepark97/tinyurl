package com.hugo.tinyurl.support.time;

import java.time.LocalDateTime;

public interface ClockProvider {

    LocalDateTime now();

}
