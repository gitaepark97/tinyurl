package com.hugo.tinyurl.shorturl.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.clickevent.port.ClickEventRepository;
import com.hugo.tinyurl.common.port.DistributedLock;
import com.hugo.tinyurl.shorturl.model.ShortUrl;
import com.hugo.tinyurl.shorturl.port.ShortUrlArchiveRepository;
import com.hugo.tinyurl.shorturl.port.ShortUrlRepository;
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
@TestPropertySource(properties = {
    "app.cleanup.short-url.chunk-size=2",
    "app.cleanup.short-url.click-event-archive-page-size=2"
})
class ExpiredShortUrlCleanerTest {

    @Autowired
    ExpiredShortUrlCleaner expiredShortUrlCleaner;

    @Autowired
    ShortUrlRepository shortUrlRepository;

    @Autowired
    ClickEventRepository clickEventRepository;

    @Autowired
    DistributedLock distributedLock;

    @MockitoBean
    ShortUrlArchiveRepository shortUrlArchiveRepository;

    @Autowired
    MeterRegistry meterRegistry;

    // 아카이빙 실패 테스트는 의도적으로 short_url을 남겨두므로, 이후 테스트가 그 잔여 데이터까지 정리하지 않도록 매번 정리한다.
    @AfterEach
    void cleanUp() {
        List<Long> ids = List.of(9_100_000L, 9_100_001L, 9_100_002L, 9_100_003L, 9_100_004L,
            9_200_000L, 9_300_000L, 9_400_000L, 9_600_000L, 9_700_000L);
        clickEventRepository.deleteAllByShortUrlIdIn(ids);
        shortUrlRepository.deleteAllById(ids);
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

    @Test
    void skipsWhenAnotherInstanceAlreadyHoldsTheCleanupLock() {
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(1);
        shortUrlRepository.save(
            new ShortUrl(9_400_000L, "explock", "https://example.com", null, expiredAt, LocalDateTime.now().minusDays(8)));

        // 다른 인스턴스가 이미 같은 락을 쥐고 있는 상황을 재현한다 - 그 상태에서 cleanUpExpired()를 호출한다.
        distributedLock.tryRun("expired-short-url-cleanup", expiredShortUrlCleaner::cleanUpExpired);

        assertThat(shortUrlRepository.findById(9_400_000L)).isPresent();
        verify(shortUrlArchiveRepository, never()).archive(any(), any(), any());
    }

    @Test
    void doesNotCleanUpShortUrlStillWithinGracePeriod() {
        LocalDateTime justExpiredAt = LocalDateTime.now().minusSeconds(5);
        shortUrlRepository.save(
            new ShortUrl(9_700_000L, "grace1", "https://example.com", null, justExpiredAt, LocalDateTime.now().minusDays(1)));

        expiredShortUrlCleaner.cleanUpExpired();

        assertThat(shortUrlRepository.findById(9_700_000L)).isPresent();
        verify(shortUrlArchiveRepository, never()).archive(any(), any(), any());
    }

    @Test
    void archivesClickEventsInPagesForHighVolumeShortUrl() {
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(1);
        Long shortUrlId = 9_600_000L;
        shortUrlRepository.save(
            new ShortUrl(shortUrlId, "highvol", "https://example.com", null, expiredAt, LocalDateTime.now().minusDays(8)));
        for (int i = 0; i < 5; i++) {
            clickEventRepository.save(
                new ClickEvent(9_600_100L + i, shortUrlId, "127.0.0.1", "test-agent", null, "dk-" + i, LocalDateTime.now().minusDays(2)));
        }

        expiredShortUrlCleaner.cleanUpExpired();

        assertThat(shortUrlRepository.findById(shortUrlId)).isEmpty();
        // click-event-archive-page-size=2, 클릭 이벤트 5건 -> 3페이지로 나뉘어 archive()가 여러 번 호출돼야 한다.
        verify(shortUrlArchiveRepository, atLeast(3)).archive(any(), any(), any());
    }

}
