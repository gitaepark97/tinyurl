package com.hugo.tinyurl.clickevent;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.clickevent.application.ClickEventTestSupport;
import com.hugo.tinyurl.clickevent.model.ClickCount;
import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.clickevent.port.ClickCountRepository;
import com.hugo.tinyurl.clickevent.port.ClickEventRepository;
import com.hugo.tinyurl.common.page.Page;
import com.hugo.tinyurl.common.page.PageParam;
import com.hugo.tinyurl.common.port.IdGenerator;
import com.hugo.tinyurl.shorturl.model.ShortUrl;
import com.hugo.tinyurl.shorturl.port.ShortUrlRepository;
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

    @Autowired
    ClickCountRepository clickCountRepository;

    @Autowired
    IdGenerator idGenerator;

    private final List<Long> createdShortUrlIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdShortUrlIds.forEach(id -> clickEventRepository.deleteAll(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, id)));
        shortUrlRepository.deleteAllById(createdShortUrlIds);
        createdShortUrlIds.clear();
    }

    private ShortUrl createShortUrl(LocalDateTime expiresAt) {
        ShortUrl shortUrl = shortUrlRepository.save(
            new ShortUrl(idGenerator.generate(), "cur" + (System.nanoTime() % 100000), "https://example.com", null, expiresAt,
                LocalDateTime.now()));
        createdShortUrlIds.add(shortUrl.id());
        return shortUrl;
    }

    private ClickEvent createClickEvent(Long shortUrlId, String ipAddress, String userAgent, String referer) {
        return clickEventRepository.save(
            new ClickEvent(idGenerator.generate(), shortUrlId, ipAddress, userAgent, referer, null, LocalDateTime.now()));
    }

    @Test
    void returnsPageWithHasNextWhenMoreEventsExist() {
        ShortUrl shortUrl = createShortUrl(LocalDateTime.now().plusDays(7));
        List<ClickEvent> saved = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            saved.add(createClickEvent(shortUrl.id(), "127.0.0.1", "agent", null));
        }

        Page<ClickEvent> page = clickEventService.findAll(shortUrl.id(), new PageParam(null, 2));

        assertThat(page.content()).extracting(ClickEvent::id)
            .containsExactly(saved.get(2).id(), saved.get(1).id());
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void returnsHasNextFalseOnLastPage() {
        ShortUrl shortUrl = createShortUrl(LocalDateTime.now().plusDays(7));
        ClickEvent event = createClickEvent(shortUrl.id(), "127.0.0.1", "agent", null);

        Page<ClickEvent> page = clickEventService.findAll(shortUrl.id(), new PageParam(null, 20));

        assertThat(page.content()).extracting(ClickEvent::id).containsExactly(event.id());
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void continuesFromGivenCursor() {
        ShortUrl shortUrl = createShortUrl(LocalDateTime.now().plusDays(7));
        List<ClickEvent> saved = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            saved.add(createClickEvent(shortUrl.id(), "127.0.0.1", "agent", null));
        }

        Page<ClickEvent> page = clickEventService.findAll(shortUrl.id(), new PageParam(saved.get(1).id(), 20));

        assertThat(page.content()).extracting(ClickEvent::id).containsExactly(saved.get(0).id());
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void returnsClickEventsForExpiredShortUrl() {
        ShortUrl expired = createShortUrl(LocalDateTime.now().minusDays(1));
        ClickEvent event = createClickEvent(expired.id(), "127.0.0.1", "agent", null);

        Page<ClickEvent> page = clickEventService.findAll(expired.id(), new PageParam(null, 20));

        assertThat(page.content()).extracting(ClickEvent::id).containsExactly(event.id());
    }

    @Test
    void returnsEmptyPageForUnknownShortUrlId() {
        // shortUrlId 존재 여부/소유자 확인은 이제 clickevent의 책임이 아니라 호출자(shorturl)가
        // 먼저 검증한다 - clickevent는 그냥 조회된 클릭 이벤트가 없으면 빈 페이지를 돌려준다.
        Page<ClickEvent> page = clickEventService.findAll(Long.MAX_VALUE, new PageParam(null, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void recordDelegatesToRecorder() {
        ShortUrl shortUrl = createShortUrl(LocalDateTime.now().plusDays(7));

        clickEventService.record(shortUrl.id(), "127.0.0.1", "agent", null, "0-1");

        assertThat(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, shortUrl.id())).singleElement();
        assertThat(clickCountRepository.findById(shortUrl.id())).get().extracting(ClickCount::count).isEqualTo(1L);
        clickCountRepository.deleteById(shortUrl.id());
    }

}
