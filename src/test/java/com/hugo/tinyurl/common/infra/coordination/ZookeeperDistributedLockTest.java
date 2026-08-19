package com.hugo.tinyurl.common.infra.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.MySqlTestcontainersConfiguration;
import com.hugo.tinyurl.ZookeeperTestcontainersConfiguration;
import com.hugo.tinyurl.common.port.DistributedLock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;

@ApplicationModuleTest(value = BootstrapMode.DIRECT_DEPENDENCIES, webEnvironment = WebEnvironment.NONE)
@Import({MySqlTestcontainersConfiguration.class, ZookeeperTestcontainersConfiguration.class})
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
