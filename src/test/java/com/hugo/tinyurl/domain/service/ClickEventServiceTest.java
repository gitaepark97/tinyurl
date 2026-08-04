package com.hugo.tinyurl.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.entity.ClickEvent;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ClickEventServiceTest {

    @Autowired
    ClickEventService clickEventService;

    @Autowired
    ShortUrlRepository shortUrlRepository;

    @Autowired
    ClickEventRepository clickEventRepository;

    private final List<Long> createdShortUrlIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdShortUrlIds.forEach(id -> clickEventRepository.deleteAll(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, id)));
        shortUrlRepository.deleteAllById(createdShortUrlIds);
        createdShortUrlIds.clear();
    }

    private ShortUrl createShortUrl(LocalDateTime expiresAt) {
        ShortUrl shortUrl = shortUrlRepository.save(
            new ShortUrl("cur" + (System.nanoTime() % 100000), "https://example.com", expiresAt));
        createdShortUrlIds.add(shortUrl.getId());
        return shortUrl;
    }

    @Test
    void returnsPageWithHasNextWhenMoreEventsExist() {
        ShortUrl shortUrl = createShortUrl(LocalDateTime.now().plusDays(7));
        List<ClickEvent> saved = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            saved.add(clickEventRepository.save(new ClickEvent(shortUrl.getId(), "127.0.0.1", "agent", null)));
        }

        Page<ClickEvent> page = clickEventService.findAll(shortUrl.getId(), new PageParam(null, 2));

        assertThat(page.content()).extracting(ClickEvent::getId)
            .containsExactly(saved.get(2).getId(), saved.get(1).getId());
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void returnsHasNextFalseOnLastPage() {
        ShortUrl shortUrl = createShortUrl(LocalDateTime.now().plusDays(7));
        ClickEvent event = clickEventRepository.save(new ClickEvent(shortUrl.getId(), "127.0.0.1", "agent", null));

        Page<ClickEvent> page = clickEventService.findAll(shortUrl.getId(), new PageParam(null, 20));

        assertThat(page.content()).extracting(ClickEvent::getId).containsExactly(event.getId());
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void continuesFromGivenCursor() {
        ShortUrl shortUrl = createShortUrl(LocalDateTime.now().plusDays(7));
        List<ClickEvent> saved = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            saved.add(clickEventRepository.save(new ClickEvent(shortUrl.getId(), "127.0.0.1", "agent", null)));
        }

        Page<ClickEvent> page = clickEventService.findAll(shortUrl.getId(), new PageParam(saved.get(1).getId(), 20));

        assertThat(page.content()).extracting(ClickEvent::getId).containsExactly(saved.get(0).getId());
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void returnsClickEventsForExpiredShortUrl() {
        ShortUrl expired = createShortUrl(LocalDateTime.now().minusDays(1));
        ClickEvent event = clickEventRepository.save(new ClickEvent(expired.getId(), "127.0.0.1", "agent", null));

        Page<ClickEvent> page = clickEventService.findAll(expired.getId(), new PageParam(null, 20));

        assertThat(page.content()).extracting(ClickEvent::getId).containsExactly(event.getId());
    }

    @Test
    void returnsEmptyPageForUnknownShortUrlId() {
        Page<ClickEvent> page = clickEventService.findAll(Long.MAX_VALUE, new PageParam(null, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

}
