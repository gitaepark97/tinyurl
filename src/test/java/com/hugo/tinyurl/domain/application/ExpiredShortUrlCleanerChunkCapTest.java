package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ShortUrlArchiveRepository;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;
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
    "app.cleanup.short-url.chunk-size=1",
    "app.cleanup.short-url.max-chunks-per-run=2"
})
class ExpiredShortUrlCleanerChunkCapTest {

    @Autowired
    ExpiredShortUrlCleaner expiredShortUrlCleaner;

    @Autowired
    ShortUrlRepository shortUrlRepository;

    @MockitoBean
    ShortUrlArchiveRepository shortUrlArchiveRepository;

    @AfterEach
    void cleanUp() {
        shortUrlRepository.deleteAllById(List.of(9_500_000L, 9_500_001L, 9_500_002L, 9_500_003L));
    }

    @Test
    void stopsAfterMaxChunksPerRunLeavingRestForNextSchedule() {
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(1);
        for (int i = 0; i < 4; i++) {
            shortUrlRepository.save(
                new ShortUrl(9_500_000L + i, "cap" + i, "https://example.com/" + i, null, expiredAt, LocalDateTime.now().minusDays(8)));
        }

        expiredShortUrlCleaner.cleanUpExpired();

        long remaining = LongStream.range(0, 4)
            .filter(i -> shortUrlRepository.findById(9_500_000L + i).isPresent())
            .count();
        // chunk-size=1, max-chunks-per-run=2 -> 4건 중 2건만 이번 실행에서 처리되고 나머지는 남아야 한다.
        assertThat(remaining).isEqualTo(2);
    }

}
