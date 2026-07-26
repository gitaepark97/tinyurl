package com.hugo.tinyurl.domain.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
class ShortKeyGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int KEY_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    String generate() {
        StringBuilder key = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            key.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return key.toString();
    }

}
