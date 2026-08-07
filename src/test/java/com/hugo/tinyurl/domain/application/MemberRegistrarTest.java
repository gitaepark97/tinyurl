package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.model.Member;
import com.hugo.tinyurl.domain.model.Role;
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
class MemberRegistrarTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberRegistrar memberRegistrar;

    Member member;

    @AfterEach
    void cleanUp() {
        if (member != null) {
            memberRepository.deleteById(member.id());
            member = null;
        }
    }

    @Test
    void registersMemberWithEncodedPasswordAndMemberRole() {
        member = memberRegistrar.register("user@example.com", "password123");

        assertThat(member.email()).isEqualTo("user@example.com");
        assertThat(member.password()).isNotEqualTo("password123");
        assertThat(member.role()).isEqualTo(Role.MEMBER);
    }

    @Test
    void rejectsDuplicateEmail() {
        member = memberRegistrar.register("dup@example.com", "password123");

        assertThatThrownBy(() -> memberRegistrar.register("dup@example.com", "password456"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.CONFLICT);
    }

}
