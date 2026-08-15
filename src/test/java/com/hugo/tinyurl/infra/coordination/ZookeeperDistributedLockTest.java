package com.hugo.tinyurl.infra.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.port.DistributedLock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ZookeeperDistributedLockTest {

    @Autowired
    DistributedLock distributedLock;

    @Test
    void skipsWhenLockAlreadyHeldByAnotherCaller() {
        AtomicBoolean innerRan = new AtomicBoolean(false);
        AtomicBoolean innerAcquired = new AtomicBoolean(true);

        boolean outerAcquired = distributedLock.tryRun("test-skip", () ->
            innerAcquired.set(distributedLock.tryRun("test-skip", () -> innerRan.set(true))));

        assertThat(outerAcquired).isTrue();
        assertThat(innerAcquired).isFalse();
        assertThat(innerRan).isFalse();
    }

    @Test
    void acquiresAgainAfterPreviousRunReleasesTheLock() {
        distributedLock.tryRun("test-reacquire", () -> {
        });

        boolean acquired = distributedLock.tryRun("test-reacquire", () -> {
        });

        assertThat(acquired).isTrue();
    }

}
