package com.hugo.tinyurl.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.entity.ClickCount;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ClickCountRepository;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
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

    ShortUrl shortUrl;

    @AfterEach
    void cleanUp() {
        if (shortUrl != null) {
            clickEventRepository.deleteAll(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, shortUrl.getId()));
            clickCountRepository.deleteById(shortUrl.getId());
            shortUrlRepository.deleteById(shortUrl.getId());
        }
    }

    @Test
    void redirectReturnsImmediatelyRegardlessOfClickRecordingOutcome() {
        shortUrl = shortUrlService.create("https://example.com");
        String oversizedUserAgent = "A".repeat(600);

        String originalUrl = shortUrlService.redirect(shortUrl.getShortKey(), "127.0.0.1", oversizedUserAgent, null);

        assertThat(originalUrl).isEqualTo("https://example.com");
    }

    @Test
    void redirectRecordsClickEventOnSuccess() {
        shortUrl = shortUrlService.create("https://example.com");

        String originalUrl = shortUrlService.redirect(shortUrl.getShortKey(), "127.0.0.1", "test-agent", "https://referer.example.com");

        assertThat(originalUrl).isEqualTo("https://example.com");
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, shortUrl.getId())).singleElement().satisfies(event -> {
                assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
                assertThat(event.getUserAgent()).isEqualTo("test-agent");
                assertThat(event.getReferer()).isEqualTo("https://referer.example.com");
            });
            assertThat(clickCountRepository.findById(shortUrl.getId()))
                .get()
                .extracting(ClickCount::getCount)
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
        shortUrl = shortUrlRepository.save(
            new ShortUrl(shortKeyGenerator.generate(), "https://example.com", LocalDateTime.now().minusDays(1)));

        assertThatThrownBy(() -> shortUrlService.redirect(shortUrl.getShortKey(), "127.0.0.1", "test-agent", null))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, shortUrl.getId())).isEmpty();
        assertThat(clickCountRepository.findById(shortUrl.getId())).isEmpty();
    }

}
