package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRegistrar memberRegistrar;

    public Member register(String email, String rawPassword) {
        return memberRegistrar.register(email, rawPassword);
    }

}
