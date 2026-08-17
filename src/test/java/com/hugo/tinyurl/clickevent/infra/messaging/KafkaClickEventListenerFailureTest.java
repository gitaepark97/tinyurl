package com.hugo.tinyurl.clickevent.infra.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.clickevent.ClickEventService;
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

    @MockitoBean
    ClickEventService clickEventService;

    @Test
    void propagatesRecordingFailureSoContainerErrorHandlerCanRetry() {
        willThrow(new RuntimeException("recording failed")).given(clickEventService)
            .record(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> kafkaClickEventListener.listen(new ClickEventMessage(1L, "127.0.0.1", "test-agent", null), 0, 0))
            .isInstanceOf(RuntimeException.class);
    }

}
