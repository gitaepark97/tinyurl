package com.hugo.tinyurl.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ClickEventRecorderTest {

    private static final long SHORT_URL_ID = 1L;

    @Autowired
    ClickEventRecorder clickEventRecorder;

    @MockitoBean
    ClickEventRepository clickEventRepository;

    @Autowired
    ClickCountRepository clickCountRepository;

    @AfterEach
    void cleanUp() {
        clickCountRepository.deleteById(SHORT_URL_ID);
    }

    @Test
    void retriesTransientFailureUntilSuccess() {
        given(clickEventRepository.save(any()))
            .willThrow(new TransientDataAccessResourceException("transient"))
            .willThrow(new TransientDataAccessResourceException("transient"))
            .willAnswer(invocation -> invocation.getArgument(0));

        clickEventRecorder.record(SHORT_URL_ID, "127.0.0.1", "test-agent", null);

        verify(clickEventRepository, times(3)).save(any());
        assertThat(clickCountRepository.findById(SHORT_URL_ID))
            .get()
            .extracting(ClickCount::getCount)
            .isEqualTo(1L);
    }

    @Test
    void propagatesAfterMaxRetriesExhausted() {
        given(clickEventRepository.save(any())).willThrow(new TransientDataAccessResourceException("transient"));

        assertThatThrownBy(() -> clickEventRecorder.record(SHORT_URL_ID, "127.0.0.1", "test-agent", null))
            .isInstanceOf(TransientDataAccessResourceException.class);

        verify(clickEventRepository, times(3)).save(any());
        assertThat(clickCountRepository.findById(SHORT_URL_ID)).isEmpty();
    }

    @Test
    void doesNotRetryNonTransientFailure() {
        given(clickEventRepository.save(any())).willThrow(new DataIntegrityViolationException("constraint violation"));

        assertThatThrownBy(() -> clickEventRecorder.record(SHORT_URL_ID, "127.0.0.1", "test-agent", null))
            .isInstanceOf(DataIntegrityViolationException.class);

        verify(clickEventRepository, times(1)).save(any());
    }

}
