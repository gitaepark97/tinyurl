package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.clickevent.model.ClickCount;
import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.clickevent.port.ClickCountRepository;
import com.hugo.tinyurl.clickevent.port.ClickEventRepository;
import com.hugo.tinyurl.common.port.ClockProvider;
import com.hugo.tinyurl.common.port.DistributedLock;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ShortUrlArchiveRepository;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Observed
@Slf4j
@Component
class ExpiredShortUrlCleaner {

    private static final String CLEANUP_LOCK_KEY = "expired-short-url-cleanup";

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final ClickCountRepository clickCountRepository;
    private final ClockProvider clockProvider;
    private final ShortUrlArchiveRepository shortUrlArchiveRepository;
    private final DistributedLock distributedLock;
    private final ExpiredShortUrlDeleter expiredShortUrlDeleter;
    private final int chunkSize;
    private final int maxChunksPerRun;
    private final int clickEventArchivePageSize;
    private final long gracePeriodMinutes;
    private final Counter archivedCounter;
    private final Counter deletedCounter;

    ExpiredShortUrlCleaner(
        ShortUrlRepository shortUrlRepository,
        ClickEventRepository clickEventRepository,
        ClickCountRepository clickCountRepository,
        ClockProvider clockProvider,
        ShortUrlArchiveRepository shortUrlArchiveRepository,
        DistributedLock distributedLock,
        ExpiredShortUrlDeleter expiredShortUrlDeleter,
        @Value("${app.cleanup.short-url.chunk-size}") int chunkSize,
        @Value("${app.cleanup.short-url.max-chunks-per-run}") int maxChunksPerRun,
        @Value("${app.cleanup.short-url.click-event-archive-page-size}") int clickEventArchivePageSize,
        @Value("${app.cleanup.short-url.grace-period-minutes}") long gracePeriodMinutes,
        MeterRegistry meterRegistry
    ) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.clickCountRepository = clickCountRepository;
        this.clockProvider = clockProvider;
        this.shortUrlArchiveRepository = shortUrlArchiveRepository;
        this.distributedLock = distributedLock;
        this.expiredShortUrlDeleter = expiredShortUrlDeleter;
        this.chunkSize = chunkSize;
        this.maxChunksPerRun = maxChunksPerRun;
        this.clickEventArchivePageSize = clickEventArchivePageSize;
        this.gracePeriodMinutes = gracePeriodMinutes;
        this.archivedCounter = Counter.builder("short_url.cleanup.archived").register(meterRegistry);
        this.deletedCounter = Counter.builder("short_url.cleanup.deleted").register(meterRegistry);
    }

    @Scheduled(cron = "${app.cleanup.short-url.cron}")
    void cleanUpExpired() {
        boolean ran = distributedLock.tryRun(CLEANUP_LOCK_KEY, this::drainExpired);
        if (!ran) {
            log.debug("다른 인스턴스가 정리 배치를 실행 중이라 이번 스케줄은 건너뜀");
        }
    }

    private void drainExpired() {
        // 클릭 기록이 Kafka 비동기 경로라 방금 만료된 URL은 아직 처리 중인 클릭이 남아있을 수 있어 유예 기간을 둔다.
        LocalDateTime cutoff = clockProvider.now().minusMinutes(gracePeriodMinutes);
        List<ShortUrl> expired;
        int processedChunks = 0;
        // 락을 무기한 붙들지 않도록 한 번 실행에서 처리할 청크 수를 제한한다 - 남은 대상은 다음 스케줄이 이어서 처리한다.
        while (processedChunks < maxChunksPerRun
            && !(expired = shortUrlRepository.findByExpiresAtBeforeOrderByIdAsc(cutoff, chunkSize)).isEmpty()) {
            archiveAndDelete(expired);
            processedChunks++;
        }
    }

    private void archiveAndDelete(List<ShortUrl> expired) {
        List<Long> shortUrlIds = expired.stream().map(ShortUrl::id).toList();
        List<ClickCount> clickCounts = clickCountRepository.findAllById(shortUrlIds);

        // 아카이빙 실패 시 예외가 전파돼 삭제로 넘어가지 않는다 - 다음 스케줄에서 같은 대상을 다시 시도한다.
        archiveInPages(expired, shortUrlIds, clickCounts);
        archivedCounter.increment(expired.size());

        expiredShortUrlDeleter.deleteAll(shortUrlIds);
        deletedCounter.increment(expired.size());
        log.info("만료된 단축 URL 정리 완료 - count={}", expired.size());
    }

    // 클릭 이벤트가 많은 배치를 한 덩어리로 메모리에 올리지 않도록 페이지 단위로 나눠 아카이빙한다.
    private void archiveInPages(List<ShortUrl> expired, List<Long> shortUrlIds, List<ClickCount> clickCounts) {
        long lastClickEventId = 0;
        List<ClickEvent> page;
        boolean archivedAnyPage = false;
        while (!(page = clickEventRepository.findByShortUrlIdInAndIdGreaterThanOrderByIdAsc(
            shortUrlIds, lastClickEventId, clickEventArchivePageSize)).isEmpty()) {
            shortUrlArchiveRepository.archive(expired, page, clickCounts);
            lastClickEventId = page.get(page.size() - 1).id();
            archivedAnyPage = true;
        }
        if (!archivedAnyPage) {
            shortUrlArchiveRepository.archive(expired, List.of(), clickCounts);
        }
    }

}
