package com.hugo.tinyurl.domain.port;

import com.hugo.tinyurl.domain.model.ClickEvent;
import java.util.List;

public interface ClickEventRepository {

    ClickEvent save(ClickEvent clickEvent);

    boolean existsByDeliveryKey(String deliveryKey);

    List<ClickEvent> findByShortUrlIdAndIdLessThanOrderByIdDesc(Long shortUrlId, Long id, int limit);

    void deleteAll(Iterable<ClickEvent> clickEvents);

}
