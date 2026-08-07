package com.hugo.tinyurl.infra.hash;

import com.hugo.tinyurl.domain.port.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordEncoder implements PasswordEncoder {

    // spring-security-crypto의 동명 클래스와 이름이 겹쳐 import 대신 완전 정규화 이름으로 위임한다.
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder delegate =
        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }

}
