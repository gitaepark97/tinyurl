package com.hugo.tinyurl.member.infra.hash;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordEncoderTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void encodesToDifferentValueThanRawPassword() {
        String encoded = passwordEncoder.encode("password123");

        assertThat(encoded).isNotEqualTo("password123");
    }

    @Test
    void matchesRawPasswordAgainstItsOwnEncodedValue() {
        String encoded = passwordEncoder.encode("password123");

        assertThat(passwordEncoder.matches("password123", encoded)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", encoded)).isFalse();
    }

}
