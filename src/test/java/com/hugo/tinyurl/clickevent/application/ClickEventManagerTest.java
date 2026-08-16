package com.hugo.tinyurl.clickevent.application;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.clickevent.port.ClickEventPublisher;
import io.micrometer.observation.tck.TestObservationRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import({TestcontainersConfiguration.class, ClickEventManagerTest.ObservationTestConfig.class})
class ClickEventManagerTest {

    @TestConfiguration
    static class ObservationTestConfig {
        @Bean
        @Primary
        TestObservationRegistry observationRegistry() {
            return TestObservationRegistry.create();
        }
    }

    @Autowired
    ClickEventManager clickEventManager;

    @Autowired
    TestObservationRegistry observationRegistry;

    @MockitoBean
    ClickEventPublisher clickEventPublisher;

    @BeforeEach
    void clearObservations() {
        observationRegistry.clear();
    }

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
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(observationRegistry).hasHandledContextsThatSatisfy(contexts -> {
            org.assertj.core.api.Assertions.assertThat(contexts)
                .filteredOn(context -> "ClickEventManager#record".equals(context.getContextualName()))
                .singleElement()
                .satisfies(context -> org.assertj.core.api.Assertions.assertThat(context.getError()).isNotNull());
        }));
    }

}
