package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ShortUrlArchiveRepository;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "app.cleanup.short-url.chunk-size=2")
class ExpiredShortUrlCleanerTest {

    @Autowired
    ExpiredShortUrlCleaner expiredShortUrlCleaner;

    @Autowired
    ShortUrlRepository shortUrlRepository;

    @MockitoBean
    ShortUrlArchiveRepository shortUrlArchiveRepository;

    @Autowired
    MeterRegistry meterRegistry;

    // 아카이빙 실패 테스트는 의도적으로 short_url을 남겨두므로, 이후 테스트가 그 잔여 데이터까지 정리하지 않도록 매번 정리한다.
    @AfterEach
    void cleanUp() {
        shortUrlRepository.deleteAllById(
            List.of(9_100_000L, 9_100_001L, 9_100_002L, 9_100_003L, 9_100_004L, 9_200_000L, 9_300_000L));
    }

    @Test
    void archivesAndDeletesAllExpiredShortUrlsAcrossChunks() {
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(1);
        LocalDateTime createdAt = LocalDateTime.now().minusDays(8);
        for (int i = 0; i < 5; i++) {
            shortUrlRepository.save(
                new ShortUrl(9_100_000L + i, "exp" + i, "https://example.com/" + i, null, expiredAt, createdAt));
        }
        double archivedBefore = meterRegistry.get("short_url.cleanup.archived").counter().count();
        double deletedBefore = meterRegistry.get("short_url.cleanup.deleted").counter().count();

        expiredShortUrlCleaner.cleanUpExpired();

        for (int i = 0; i < 5; i++) {
            assertThat(shortUrlRepository.findById(9_100_000L + i)).isEmpty();
        }
        assertThat(meterRegistry.get("short_url.cleanup.archived").counter().count()).isEqualTo(archivedBefore + 5);
        assertThat(meterRegistry.get("short_url.cleanup.deleted").counter().count()).isEqualTo(deletedBefore + 5);
    }

    @Test
    void doesNotDeleteWhenArchivingFails() {
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(1);
        shortUrlRepository.save(
            new ShortUrl(9_200_000L, "exp-fail", "https://example.com", null, expiredAt, LocalDateTime.now().minusDays(8)));
        willThrow(new RuntimeException("s3 down")).given(shortUrlArchiveRepository).archive(any(), any(), any());

        assertThatThrownBy(() -> expiredShortUrlCleaner.cleanUpExpired()).isInstanceOf(RuntimeException.class);

        assertThat(shortUrlRepository.findById(9_200_000L)).isPresent();
    }

    @Test
    void doesNotTouchNonExpiredShortUrls() {
        LocalDateTime notExpiredAt = LocalDateTime.now().plusDays(1);
        shortUrlRepository.save(
            new ShortUrl(9_300_000L, "active1", "https://example.com", null, notExpiredAt, LocalDateTime.now()));

        expiredShortUrlCleaner.cleanUpExpired();

        assertThat(shortUrlRepository.findById(9_300_000L)).isPresent();
        verify(shortUrlArchiveRepository, never()).archive(any(), any(), any());
    }

}
