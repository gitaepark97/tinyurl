package com.hugo.tinyurl.clickevent.port;

import com.hugo.tinyurl.clickevent.model.ClickCount;
import java.util.List;
import java.util.Optional;

public interface ClickCountRepository {

    void increment(Long shortUrlId);

    Optional<ClickCount> findById(Long shortUrlId);

    List<ClickCount> findAllById(Iterable<Long> shortUrlIds);

    void deleteById(Long shortUrlId);

    void deleteAllById(Iterable<Long> shortUrlIds);

}
