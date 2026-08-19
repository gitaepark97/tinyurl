package com.hugo.tinyurl.common.infra.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.MySqlTestcontainersConfiguration;
import com.hugo.tinyurl.ZookeeperTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;

@ApplicationModuleTest(value = BootstrapMode.DIRECT_DEPENDENCIES, webEnvironment = WebEnvironment.NONE)
@Import({MySqlTestcontainersConfiguration.class, ZookeeperTestcontainersConfiguration.class})
class ZookeeperConfigTest {

    @Autowired
    Long workerId;

    @Test
    void workerIdIsAssignedWithinValidRange() {
        assertThat(workerId).isBetween(0L, 1023L);
    }

}
