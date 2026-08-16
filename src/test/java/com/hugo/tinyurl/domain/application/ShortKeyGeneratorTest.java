package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hugo.tinyurl.common.port.Counter;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortKeyGeneratorTest {

    @Mock
    Counter counter;

    @Test
    void generatesEightCharacterBase62Key() {
        when(counter.next()).thenReturn(123456789L);
        ShortKeyGenerator generator = new ShortKeyGenerator(counter);

        String key = generator.generate();

        assertThat(key).hasSize(8).matches("[0-9A-Za-z]{8}");
    }

    @Test
    void producesDistinctKeysForDistinctCounterValues() {
        ShortKeyGenerator generator = new ShortKeyGenerator(counter);
        Set<String> keys = new HashSet<>();

        for (long value = 0; value < 10_000; value++) {
            when(counter.next()).thenReturn(value);
            keys.add(generator.generate());
        }

        assertThat(keys).hasSize(10_000);
    }

}
