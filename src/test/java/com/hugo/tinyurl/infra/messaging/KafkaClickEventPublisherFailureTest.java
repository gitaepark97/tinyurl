package com.hugo.tinyurl.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.port.ClickEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// kafkaTemplate.send()가 브로커 통신 실패를 대부분 whenComplete의 비동기 콜백으로만 알려주므로,
// 실제 Kafka 없이 KafkaTemplate만 목으로 대체해 그 콜백 경로를 검증한다.
@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class KafkaClickEventPublisherFailureTest {

    @Autowired
    ClickEventPublisher clickEventPublisher;

    @Autowired
    MeterRegistry meterRegistry;

    @MockitoBean
    KafkaTemplate<String, ClickEventMessage> kafkaTemplate;

    @Test
    void incrementsFailureCounterWhenSendCompletesExceptionally() {
        given(kafkaTemplate.send(any(String.class), any(String.class), any(ClickEventMessage.class)))
            .willReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));
        double before = meterRegistry.get("click_event.publish.failure").counter().count();

        clickEventPublisher.publish(1L, "127.0.0.1", "test-agent", null);

        assertThat(meterRegistry.get("click_event.publish.failure").counter().count()).isEqualTo(before + 1);
    }

    @Test
    void incrementsFailureCounterWhenSendThrowsSynchronously() {
        given(kafkaTemplate.send(any(String.class), any(String.class), any(ClickEventMessage.class)))
            .willThrow(new RuntimeException("kafka unreachable"));
        double before = meterRegistry.get("click_event.publish.failure").counter().count();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> clickEventPublisher.publish(1L, "127.0.0.1", "test-agent", null))
            .isInstanceOf(RuntimeException.class);

        assertThat(meterRegistry.get("click_event.publish.failure").counter().count()).isEqualTo(before + 1);
    }

}
