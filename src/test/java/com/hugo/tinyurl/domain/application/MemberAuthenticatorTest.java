package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.model.Member;
import com.hugo.tinyurl.domain.port.MemberRepository;
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
class MemberAuthenticatorTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberRegistrar memberRegistrar;

    @Autowired
    MemberAuthenticator memberAuthenticator;

    Member member;

    @AfterEach
    void cleanUp() {
        if (member != null) {
            memberRepository.deleteById(member.id());
            member = null;
        }
    }

    @Test
    void authenticatesWithCorrectCredentials() {
        member = memberRegistrar.register("user@example.com", "password123");

        Member authenticated = memberAuthenticator.authenticate("user@example.com", "password123");

        assertThat(authenticated.id()).isEqualTo(member.id());
    }

    @Test
    void rejectsWrongPassword() {
        member = memberRegistrar.register("user@example.com", "password123");

        assertThatThrownBy(() -> memberAuthenticator.authenticate("user@example.com", "wrong-password"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void rejectsUnknownEmail() {
        assertThatThrownBy(() -> memberAuthenticator.authenticate("nobody@example.com", "password123"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void findsMemberById() {
        member = memberRegistrar.register("user@example.com", "password123");

        Member found = memberAuthenticator.findById(member.id());

        assertThat(found.id()).isEqualTo(member.id());
    }

    @Test
    void rejectsUnknownId() {
        assertThatThrownBy(() -> memberAuthenticator.findById(Long.MAX_VALUE))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

}
