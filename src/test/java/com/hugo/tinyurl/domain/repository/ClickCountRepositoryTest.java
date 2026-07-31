package com.hugo.tinyurl.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.entity.ClickCount;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ClickCountRepositoryTest {

    private static final long SHORT_URL_ID = 1L;

    @Autowired
    ClickCountRepository clickCountRepository;

    @AfterEach
    void cleanUp() {
        clickCountRepository.deleteById(SHORT_URL_ID);
    }

    @Test
    void incrementsCountAtomicallyUnderConcurrentFirstClicks() throws Exception {
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    clickCountRepository.increment(SHORT_URL_ID);
                } finally {
                    latch.countDown();
                }
            }));
        }
        latch.await();
        executor.shutdown();
        for (Future<?> future : futures) {
            future.get();
        }

        assertThat(clickCountRepository.findById(SHORT_URL_ID))
            .get()
            .extracting(ClickCount::getCount)
            .isEqualTo((long) threads);
    }

}
