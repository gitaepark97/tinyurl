package com.hugo.tinyurl.member.application;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.port.MemberRepository;
import com.hugo.tinyurl.member.port.PasswordEncoder;
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
