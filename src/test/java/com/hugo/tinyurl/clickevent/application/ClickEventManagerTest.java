package com.hugo.tinyurl.clickevent.application;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.clickevent.port.ClickEventPublisher;
import io.micrometer.observation.tck.TestObservationRegistry;
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

// record()는 더 이상 @Async가 아니다 - 호출자(ClickEventVisitListener)가 이미 비동기 경계이고,
// record() 자체가 실패해야 Spring Modulith가 이벤트 발행을 미완료로 기록하고 재시도할 수 있다.
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
    void recordDelegatesToPublisherSynchronously() {
        clickEventManager.record(1L, "127.0.0.1", "test-agent", "https://referer.example.com");

        verify(clickEventPublisher).publish(eq(1L), eq("127.0.0.1"), eq("test-agent"), eq("https://referer.example.com"));
    }

    @Test
    void propagatesPublishFailureInsteadOfSwallowing() {
        RuntimeException publishFailure = new RuntimeException("publish failed");
        willThrow(publishFailure).given(clickEventPublisher).publish(any(), any(), any(), any());

        assertThatThrownBy(() -> clickEventManager.record(1L, "127.0.0.1", "test-agent", null))
            .isSameAs(publishFailure);

        // @Observed가 전파된 예외를 자동으로 span에 기록하는지도 함께 확인한다.
        assertThat(observationRegistry).hasHandledContextsThatSatisfy(contexts -> {
            org.assertj.core.api.Assertions.assertThat(contexts)
                .filteredOn(context -> "ClickEventManager#record".equals(context.getContextualName()))
                .singleElement()
                .satisfies(context -> org.assertj.core.api.Assertions.assertThat(context.getError()).isNotNull());
        });
    }

}
