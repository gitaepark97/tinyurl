package com.hugo.tinyurl.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.entity.ClickCount;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ClickCountRepository;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
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
class ShortUrlServiceTest {

    @Autowired
    ShortUrlService shortUrlService;

    @Autowired
    ShortUrlRepository shortUrlRepository;

    @Autowired
    ClickEventRepository clickEventRepository;

    @Autowired
    ClickCountRepository clickCountRepository;

    private final List<Long> createdShortUrlIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        shortUrlRepository.deleteAllById(createdShortUrlIds);
        createdShortUrlIds.clear();
    }

    private ShortUrl create(String originalUrl) {
        ShortUrl shortUrl = shortUrlService.create(originalUrl);
        createdShortUrlIds.add(shortUrl.getId());
        return shortUrl;
    }

    @Test
    void createsShortUrlWithSevenDayExpiration() {
        ShortUrl shortUrl = create("https://example.com");

        assertThat(shortUrl.getShortKey()).hasSize(8);
        assertThat(shortUrl.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(shortUrl.isExpired(LocalDateTime.now())).isFalse();
    }

    @Test
    void issuesNewShortKeyForSameOriginalUrl() {
        ShortUrl first = create("https://example.com");
        ShortUrl second = create("https://example.com");

        assertThat(first.getShortKey()).isNotEqualTo(second.getShortKey());
    }

    @Test
    void returnsOriginalUrlForValidKey() {
        ShortUrl shortUrl = create("https://example.com");

        String originalUrl = shortUrlService.redirect(shortUrl.getShortKey(), "127.0.0.1", "test-agent", null);

        assertThat(originalUrl).isEqualTo("https://example.com");
    }

    @Test
    void recordsClickEventOnSuccessfulRedirect() {
        ShortUrl shortUrl = create("https://example.com");

        shortUrlService.redirect(shortUrl.getShortKey(), "127.0.0.1", "test-agent", "https://referer.example.com");

        assertThat(clickEventRepository.findByShortUrlId(shortUrl.getId())).singleElement().satisfies(event -> {
            assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
            assertThat(event.getUserAgent()).isEqualTo("test-agent");
            assertThat(event.getReferer()).isEqualTo("https://referer.example.com");
        });
        assertThat(clickCountRepository.findById(shortUrl.getId()))
            .get()
            .extracting(ClickCount::getCount)
            .isEqualTo(1L);
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
        ShortUrl expired = shortUrlRepository.save(
            new ShortUrl("exp12345", "https://example.com", LocalDateTime.now().minusDays(1)));
        createdShortUrlIds.add(expired.getId());

        assertThatThrownBy(() -> shortUrlService.redirect(expired.getShortKey(), "127.0.0.1", "test-agent", null))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);

        assertThat(clickEventRepository.findByShortUrlId(expired.getId())).isEmpty();
    }

}
