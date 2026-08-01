package com.hugo.tinyurl.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.repository.ClickCountRepository;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
import com.hugo.tinyurl.domain.repository.ShortUrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ShortUrlServiceResilienceTest {

    @Autowired
    ShortUrlService shortUrlService;

    @Autowired
    ShortUrlRepository shortUrlRepository;

    @Autowired
    ClickEventRepository clickEventRepository;

    @Autowired
    ClickCountRepository clickCountRepository;

    ShortUrl shortUrl;

    @AfterEach
    void cleanUp() {
        if (shortUrl != null) {
            shortUrlRepository.deleteById(shortUrl.getId());
        }
    }

    @Test
    void redirectSucceedsEvenWhenClickRecordingFails() {
        shortUrl = shortUrlService.create("https://example.com");
        String oversizedUserAgent = "A".repeat(600);

        String originalUrl = shortUrlService.redirect(shortUrl.getShortKey(), "127.0.0.1", oversizedUserAgent, null);

        assertThat(originalUrl).isEqualTo("https://example.com");
        assertThat(clickEventRepository.findByShortUrlId(shortUrl.getId())).isEmpty();
        assertThat(clickCountRepository.findById(shortUrl.getId())).isEmpty();
    }

}
