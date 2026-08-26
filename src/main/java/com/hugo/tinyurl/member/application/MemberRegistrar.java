package com.hugo.tinyurl.member.application;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.port.ClockProvider;
import com.hugo.tinyurl.common.port.IdGenerator;
import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.port.MemberRepository;
import com.hugo.tinyurl.member.port.PasswordEncoder;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Observed
@Slf4j
@Component
@RequiredArgsConstructor
class MemberRegistrar {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClockProvider clockProvider;
    private final IdGenerator idGenerator;

    Member register(String email, String rawPassword) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        Member member = Member.create(idGenerator.generate(), email, passwordEncoder.encode(rawPassword), clockProvider.now());
        try {
            return memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            // 이메일 중복은 위에서 걸러졌으므로, 여기서 남는 원인은 Snowflake id(PK) 충돌뿐이다.
            log.error("Member 저장 실패 - id={}, email={}", member.id(), member.email(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

}
