package com.hugo.tinyurl.member.application;

import com.hugo.tinyurl.common.model.Role;
import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.port.MemberRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Observed
@Component
@RequiredArgsConstructor
class MemberRoleUpdater {

    private final MemberRepository memberRepository;

    Member updateRole(Long id, Role role) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (member.role() == Role.ADMIN && role != Role.ADMIN && memberRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_DEMOTION);
        }
        memberRepository.updateRole(id, role);
        return new Member(member.id(), member.email(), member.password(), role, member.createdAt());
    }

}
