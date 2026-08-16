package com.hugo.tinyurl.clickevent.port;

import com.hugo.tinyurl.clickevent.model.ClickEvent;
import java.util.List;

public interface ClickEventRepository {

    ClickEvent save(ClickEvent clickEvent);

    boolean existsByDeliveryKey(String deliveryKey);

    List<ClickEvent> findByShortUrlIdAndIdLessThanOrderByIdDesc(Long shortUrlId, Long id, int limit);

    List<ClickEvent> findByShortUrlIdInAndIdGreaterThanOrderByIdAsc(List<Long> shortUrlIds, Long id, int limit);

    void deleteAll(Iterable<ClickEvent> clickEvents);

    void deleteAllByShortUrlIdIn(List<Long> shortUrlIds);

}
