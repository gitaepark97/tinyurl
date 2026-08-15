package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.ClickCount;
import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.domain.port.ClickEventRepository;
import com.hugo.tinyurl.domain.port.ClockProvider;
import com.hugo.tinyurl.domain.port.ShortUrlArchiveRepository;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Observed
@Slf4j
@Component
class ExpiredShortUrlCleaner {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final ClickCountRepository clickCountRepository;
    private final ClockProvider clockProvider;
    private final ShortUrlArchiveRepository shortUrlArchiveRepository;
    private final ExpiredShortUrlDeleter expiredShortUrlDeleter;
    private final int chunkSize;
    private final Counter archivedCounter;
    private final Counter deletedCounter;

    ExpiredShortUrlCleaner(
        ShortUrlRepository shortUrlRepository,
        ClickEventRepository clickEventRepository,
        ClickCountRepository clickCountRepository,
        ClockProvider clockProvider,
        ShortUrlArchiveRepository shortUrlArchiveRepository,
        ExpiredShortUrlDeleter expiredShortUrlDeleter,
        @Value("${app.cleanup.short-url.chunk-size}") int chunkSize,
        MeterRegistry meterRegistry
    ) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.clickCountRepository = clickCountRepository;
        this.clockProvider = clockProvider;
        this.shortUrlArchiveRepository = shortUrlArchiveRepository;
        this.expiredShortUrlDeleter = expiredShortUrlDeleter;
        this.chunkSize = chunkSize;
        this.archivedCounter = Counter.builder("short_url.cleanup.archived").register(meterRegistry);
        this.deletedCounter = Counter.builder("short_url.cleanup.deleted").register(meterRegistry);
    }

    @Scheduled(cron = "${app.cleanup.short-url.cron}")
    void cleanUpExpired() {
        List<ShortUrl> expired;
        while (!(expired = shortUrlRepository.findByExpiresAtBeforeOrderByIdAsc(clockProvider.now(), chunkSize)).isEmpty()) {
            archiveAndDelete(expired);
        }
    }

    private void archiveAndDelete(List<ShortUrl> expired) {
        List<Long> shortUrlIds = expired.stream().map(ShortUrl::id).toList();
        List<ClickEvent> clickEvents = clickEventRepository.findAllByShortUrlIdIn(shortUrlIds);
        List<ClickCount> clickCounts = clickCountRepository.findAllById(shortUrlIds);

        // 아카이빙 실패 시 예외가 전파돼 삭제로 넘어가지 않는다 - 다음 스케줄에서 같은 대상을 다시 시도한다.
        shortUrlArchiveRepository.archive(expired, clickEvents, clickCounts);
        archivedCounter.increment(expired.size());

        expiredShortUrlDeleter.deleteAll(shortUrlIds, clickEvents);
        deletedCounter.increment(expired.size());
        log.info("만료된 단축 URL 정리 완료 - count={}", expired.size());
    }

}
