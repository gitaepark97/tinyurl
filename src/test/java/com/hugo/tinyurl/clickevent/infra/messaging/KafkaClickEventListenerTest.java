package com.hugo.tinyurl.clickevent.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.clickevent.model.ClickCount;
import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.clickevent.port.ClickCountRepository;
import com.hugo.tinyurl.clickevent.port.ClickEventPublisher;
import com.hugo.tinyurl.clickevent.port.ClickEventRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class KafkaClickEventListenerTest {

    private static final long SHORT_URL_ID = 2_000_000L;

    @Autowired
    ClickEventPublisher clickEventPublisher;

    @Autowired
    ClickEventRepository clickEventRepository;

    @Autowired
    ClickCountRepository clickCountRepository;

    @AfterEach
    void cleanUp() {
        List<ClickEvent> events = clickEventRepository.findByShortUrlIdAndIdLessThanOrderByIdDesc(SHORT_URL_ID, Long.MAX_VALUE, Integer.MAX_VALUE);
        clickEventRepository.deleteAll(events);
        clickCountRepository.deleteById(SHORT_URL_ID);
    }

    @Test
    void consumesPublishedMessageAndRecordsClickEvent() {
        clickEventPublisher.publish(SHORT_URL_ID, "127.0.0.1", "test-agent", "https://referer.example.com");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ClickEvent> events =
                clickEventRepository.findByShortUrlIdAndIdLessThanOrderByIdDesc(SHORT_URL_ID, Long.MAX_VALUE, Integer.MAX_VALUE);
            assertThat(events).singleElement().satisfies(event -> {
                assertThat(event.ipAddress()).isEqualTo("127.0.0.1");
                assertThat(event.userAgent()).isEqualTo("test-agent");
                assertThat(event.referer()).isEqualTo("https://referer.example.com");
                assertThat(event.deliveryKey()).isNotBlank();
            });
            assertThat(clickCountRepository.findById(SHORT_URL_ID))
                .get()
                .extracting(ClickCount::count)
                .isEqualTo(1L);
        });
    }

}
