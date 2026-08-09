package com.hugo.tinyurl.infra.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.port.Counter;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ZookeeperCounterTest {

    @Autowired
    Counter counter;

    @Test
    void returnsMonotonicallyIncreasingDistinctValues() {
        Set<Long> values = new HashSet<>();
        long previous = -1;

        for (int i = 0; i < 100; i++) {
            long value = counter.next();
            assertThat(value).isGreaterThan(previous);
            previous = value;
            values.add(value);
        }

        assertThat(values).hasSize(100);
    }

}
