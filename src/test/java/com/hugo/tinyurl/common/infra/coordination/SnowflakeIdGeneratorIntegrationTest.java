package com.hugo.tinyurl.common.infra.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.MySqlTestcontainersConfiguration;
import com.hugo.tinyurl.ZookeeperTestcontainersConfiguration;
import com.hugo.tinyurl.common.port.IdGenerator;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;

@ApplicationModuleTest(value = BootstrapMode.DIRECT_DEPENDENCIES, webEnvironment = WebEnvironment.NONE)
@Import({MySqlTestcontainersConfiguration.class, ZookeeperTestcontainersConfiguration.class})
class SnowflakeIdGeneratorIntegrationTest {

    @Autowired
    IdGenerator idGenerator;

    @Test
    void generatesDistinctIdsViaZooKeeperAssignedWorkerId() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add(idGenerator.generate());
        }

        assertThat(ids).hasSize(100);
    }

}
