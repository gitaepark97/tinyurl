package com.hugo.tinyurl.domain.port;

import com.hugo.tinyurl.domain.model.ClickCount;
import java.util.List;
import java.util.Optional;

public interface ClickCountRepository {

    void increment(Long shortUrlId);

    Optional<ClickCount> findById(Long shortUrlId);

    List<ClickCount> findAllById(Iterable<Long> shortUrlIds);

    void deleteById(Long shortUrlId);

}
