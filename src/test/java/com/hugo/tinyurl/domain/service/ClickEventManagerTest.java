package com.hugo.tinyurl.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.entity.ClickCount;
import com.hugo.tinyurl.domain.repository.ClickCountRepository;
import com.hugo.tinyurl.domain.repository.ClickEventRepository;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ClickEventManagerTest {

    private static final long SHORT_URL_ID = 1L;
    private static final long UNUSED_SHORT_URL_ID = 999L;

    @Autowired
    ClickEventManager clickEventManager;

    @Autowired
    ClickEventRepository clickEventRepository;

    @Autowired
    ClickCountRepository clickCountRepository;

    @AfterEach
    void cleanUp() {
        clickEventRepository.deleteAll(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, SHORT_URL_ID));
        clickCountRepository.deleteById(SHORT_URL_ID);
    }

    @Test
    void recordReturnsImmediatelyAndEventuallyPersists() {
        long start = System.nanoTime();

        clickEventManager.record(SHORT_URL_ID, "127.0.0.1", "test-agent", "https://referer.example.com");

        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMillis).isLessThan(50);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(ClickEventTestSupport.findAllByShortUrlId(clickEventRepository, SHORT_URL_ID)).singleElement();
            assertThat(clickCountRepository.findById(SHORT_URL_ID))
                .get()
                .extracting(ClickCount::getCount)
                .isEqualTo(1L);
        });
    }

    @Test
    void swallowsPermanentFailureInsteadOfPropagating() {
        clickEventManager.record(null, "127.0.0.1", "test-agent", null);

        // 실패 케이스는 "끝내 기록되지 않음"을 증명해야 하므로, 같은 스레드풀에 뒤이어 제출한
        // 정상 케이스가 먼저 반영되는 걸 기다려 비동기 처리가 실제로 끝났음을 보장한다.
        clickEventManager.record(UNUSED_SHORT_URL_ID, "127.0.0.1", "test-agent", null);
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> assertThat(clickCountRepository.findById(UNUSED_SHORT_URL_ID)).isPresent());
        clickCountRepository.deleteById(UNUSED_SHORT_URL_ID);

        assertThat(clickCountRepository.findById(SHORT_URL_ID)).isEmpty();
    }

}
