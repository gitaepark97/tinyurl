package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.Member;
import com.hugo.tinyurl.domain.port.MemberRepository;
import com.hugo.tinyurl.domain.port.PasswordEncoder;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Observed
@Component
@RequiredArgsConstructor
class MemberAuthenticator {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    Member authenticate(String email, String rawPassword) {
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!passwordEncoder.matches(rawPassword, member.password())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return member;
    }

    Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

}
