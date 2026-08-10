package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.port.ClickEventPublisher;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ClickEventManagerTest {

    @Autowired
    ClickEventManager clickEventManager;

    @MockitoBean
    ClickEventPublisher clickEventPublisher;

    @Test
    void recordReturnsImmediatelyAndEventuallyDelegatesToPublisher() {
        long start = System.nanoTime();

        clickEventManager.record(1L, "127.0.0.1", "test-agent", "https://referer.example.com");

        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMillis).isLessThan(50);
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> verify(clickEventPublisher).publish(eq(1L), eq("127.0.0.1"), eq("test-agent"), eq("https://referer.example.com")));
    }

    @Test
    void swallowsPublishFailureInsteadOfPropagating() {
        willThrow(new RuntimeException("publish failed")).given(clickEventPublisher).publish(any(), any(), any(), any());

        clickEventManager.record(1L, "127.0.0.1", "test-agent", null);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> verify(clickEventPublisher).publish(any(), any(), any(), any()));
    }

}
