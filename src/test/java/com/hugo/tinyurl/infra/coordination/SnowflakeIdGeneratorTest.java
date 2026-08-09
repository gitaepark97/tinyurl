package com.hugo.tinyurl.infra.coordination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.support.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SnowflakeIdGeneratorTest {

    @Test
    void generatesMonotonicallyIncreasingDistinctIds() {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1);
        long previous = -1;

        for (int i = 0; i < 10_000; i++) {
            long id = idGenerator.generate();
            assertThat(id).isGreaterThan(previous);
            previous = id;
        }
    }

    @Test
    void generatesDistinctIdsUnderConcurrency() throws Exception {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1);
        int threadCount = 16;
        int idsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<List<Long>>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> executor.submit(() -> {
                    List<Long> ids = new ArrayList<>(idsPerThread);
                    for (int j = 0; j < idsPerThread; j++) {
                        ids.add(idGenerator.generate());
                    }
                    return ids;
                }))
                .toList();

            Set<Long> allIds = new HashSet<>();
            for (Future<List<Long>> future : futures) {
                allIds.addAll(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(allIds).hasSize(threadCount * idsPerThread);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void rejectsWorkerIdOutOfRange() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1024)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void waitsOutSmallBackwardClockDriftAndContinues() {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1);
        ReflectionTestUtils.setField(idGenerator, "lastTimestamp", System.currentTimeMillis() + 2);

        long id = idGenerator.generate();

        assertThat(id).isPositive();
    }

    @Test
    void rejectsLargeBackwardClockDrift() {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1);
        ReflectionTestUtils.setField(idGenerator, "lastTimestamp", System.currentTimeMillis() + 100);

        assertThatThrownBy(idGenerator::generate).isInstanceOf(BusinessException.class);
    }

}
