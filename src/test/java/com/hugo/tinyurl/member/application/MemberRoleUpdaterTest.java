package com.hugo.tinyurl.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.model.Role;
import com.hugo.tinyurl.member.port.MemberRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class MemberRoleUpdaterTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberRegistrar memberRegistrar;

    @Autowired
    MemberRoleUpdater memberRoleUpdater;

    Member member;

    @AfterEach
    void cleanUp() {
        if (member != null) {
            memberRepository.deleteById(member.id());
            member = null;
        }
    }

    @Test
    void promotesMemberToAdmin() {
        member = memberRegistrar.register("user@example.com", "password123");

        Member updated = memberRoleUpdater.updateRole(member.id(), Role.ADMIN);

        assertThat(updated.role()).isEqualTo(Role.ADMIN);
        assertThat(memberRepository.findById(member.id())).get().extracting(Member::role).isEqualTo(Role.ADMIN);
    }

    @Test
    void rejectsDemotingLastAdmin() {
        member = memberRegistrar.register("user@example.com", "password123");
        memberRoleUpdater.updateRole(member.id(), Role.ADMIN);

        assertThatThrownBy(() -> memberRoleUpdater.updateRole(member.id(), Role.MEMBER))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.LAST_ADMIN_DEMOTION);
    }

    @Test
    void rejectsUnknownId() {
        assertThatThrownBy(() -> memberRoleUpdater.updateRole(Long.MAX_VALUE, Role.ADMIN))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

}
