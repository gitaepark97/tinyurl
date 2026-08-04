package com.hugo.tinyurl.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.entity.ClickCount;
import com.hugo.tinyurl.domain.repository.ClickCountRepository;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ClickEventManagerTest {

    private static final long SHORT_URL_ID_1 = 1L;
    private static final long SHORT_URL_ID_2 = 2L;

    @Autowired
    ClickEventManager clickEventManager;

    @Autowired
    ClickEventRepository clickEventRepository;

    @Autowired
    ClickCountRepository clickCountRepository;

    @AfterEach
    void cleanUp() {
        clickEventRepository.deleteAll(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, SHORT_URL_ID_1));
        clickEventRepository.deleteAll(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, SHORT_URL_ID_2));
        clickCountRepository.deleteById(SHORT_URL_ID_1);
        clickCountRepository.deleteById(SHORT_URL_ID_2);
    }

    @Test
    void recordsEventAndIncrementsCount() {
        clickEventManager.record(SHORT_URL_ID_1, "127.0.0.1", "test-agent", "https://referer.example.com");

        assertThat(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, SHORT_URL_ID_1)).singleElement().satisfies(event -> {
            assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
            assertThat(event.getUserAgent()).isEqualTo("test-agent");
            assertThat(event.getReferer()).isEqualTo("https://referer.example.com");
        });
        assertThat(clickCountRepository.findById(SHORT_URL_ID_1))
            .get()
            .extracting(ClickCount::getCount)
            .isEqualTo(1L);
    }

    @Test
    void accumulatesCountAcrossMultipleClicks() {
        clickEventManager.record(SHORT_URL_ID_2, "127.0.0.1", "test-agent", null);
        clickEventManager.record(SHORT_URL_ID_2, "127.0.0.1", "test-agent", null);

        assertThat(clickCountRepository.findById(SHORT_URL_ID_2))
            .get()
            .extracting(ClickCount::getCount)
            .isEqualTo(2L);
    }

    @Test
    void propagatesFailureInsteadOfSwallowing() {
        assertThatThrownBy(() -> clickEventManager.record(null, "127.0.0.1", "test-agent", null))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

}
