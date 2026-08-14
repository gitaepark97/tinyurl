package com.hugo.tinyurl.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.application.ClickEventService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class KafkaClickEventListenerFailureTest {

    @Autowired
    KafkaClickEventListener kafkaClickEventListener;

    @Autowired
    MeterRegistry meterRegistry;

    @MockitoBean
    ClickEventService clickEventService;

    @Test
    void swallowsRecordingFailureInsteadOfPropagating() {
        willThrow(new RuntimeException("recording failed")).given(clickEventService)
            .record(any(), any(), any(), any(), any());
        double before = meterRegistry.get("click_event.consume.failure").counter().count();

        assertThatCode(() -> kafkaClickEventListener.listen(new ClickEventMessage(1L, "127.0.0.1", "test-agent", null), 0, 0))
            .doesNotThrowAnyException();

        assertThat(meterRegistry.get("click_event.consume.failure").counter().count()).isEqualTo(before + 1);
    }

}
