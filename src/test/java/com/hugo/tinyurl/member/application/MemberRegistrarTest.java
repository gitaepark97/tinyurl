package com.hugo.tinyurl.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.MySqlTestcontainersConfiguration;
import com.hugo.tinyurl.RedisTestcontainersConfiguration;
import com.hugo.tinyurl.ZookeeperTestcontainersConfiguration;
import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.model.Role;
import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.port.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;

@ApplicationModuleTest(value = BootstrapMode.DIRECT_DEPENDENCIES, webEnvironment = WebEnvironment.NONE)
@Import({
    MySqlTestcontainersConfiguration.class,
    RedisTestcontainersConfiguration.class,
    ZookeeperTestcontainersConfiguration.class
})
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
