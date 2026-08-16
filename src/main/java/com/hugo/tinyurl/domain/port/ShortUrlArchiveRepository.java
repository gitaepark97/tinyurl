package com.hugo.tinyurl.domain.port;

import com.hugo.tinyurl.clickevent.model.ClickCount;
import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.domain.model.ShortUrl;
import java.util.List;

public interface ShortUrlArchiveRepository {

    void archive(List<ShortUrl> shortUrls, List<ClickEvent> clickEvents, List<ClickCount> clickCounts);

}
