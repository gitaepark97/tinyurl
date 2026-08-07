package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.model.ClickCount;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.domain.port.ClickEventRepository;
import com.hugo.tinyurl.domain.port.IdGenerator;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ShortUrlServiceTest {

    @Autowired
    ShortUrlRepository shortUrlRepository;

    @Autowired
    ClickEventRepository clickEventRepository;

    @Autowired
    ClickCountRepository clickCountRepository;

    @Autowired
    ShortUrlService shortUrlService;

    @Autowired
    ShortKeyGenerator shortKeyGenerator;

    @Autowired
    IdGenerator idGenerator;

    ShortUrl shortUrl;

    @AfterEach
    void cleanUp() {
        if (shortUrl != null) {
            clickEventRepository.deleteAll(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, shortUrl.id()));
            clickCountRepository.deleteById(shortUrl.id());
            shortUrlRepository.deleteById(shortUrl.id());
        }
    }

    @Test
    void redirectReturnsImmediatelyRegardlessOfClickRecordingOutcome() {
        shortUrl = shortUrlService.create("https://example.com");
        String oversizedUserAgent = "A".repeat(600);

        String originalUrl = shortUrlService.redirect(shortUrl.shortKey(), "127.0.0.1", oversizedUserAgent, null);

        assertThat(originalUrl).isEqualTo("https://example.com");
    }

    @Test
    void redirectRecordsClickEventOnSuccess() {
        shortUrl = shortUrlService.create("https://example.com");

        String originalUrl = shortUrlService.redirect(shortUrl.shortKey(), "127.0.0.1", "test-agent", "https://referer.example.com");

        assertThat(originalUrl).isEqualTo("https://example.com");
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, shortUrl.id())).singleElement().satisfies(event -> {
                assertThat(event.ipAddress()).isEqualTo("127.0.0.1");
                assertThat(event.userAgent()).isEqualTo("test-agent");
                assertThat(event.referer()).isEqualTo("https://referer.example.com");
            });
            assertThat(clickCountRepository.findById(shortUrl.id()))
                .get()
                .extracting(ClickCount::count)
                .isEqualTo(1L);
        });
    }

    @Test
    void throwsNotFoundForUnknownKeyWithoutRecordingClick() {
        assertThatThrownBy(() -> shortUrlService.redirect("nope0000", "127.0.0.1", "test-agent", null))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void throwsNotFoundForExpiredKeyWithoutRecordingClick() {
        LocalDateTime now = LocalDateTime.now();
        shortUrl = shortUrlRepository.save(
            new ShortUrl(idGenerator.generate(), shortKeyGenerator.generate(), "https://example.com", now.minusDays(1), now));

        assertThatThrownBy(() -> shortUrlService.redirect(shortUrl.shortKey(), "127.0.0.1", "test-agent", null))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, shortUrl.id())).isEmpty();
        assertThat(clickCountRepository.findById(shortUrl.id())).isEmpty();
    }

}
