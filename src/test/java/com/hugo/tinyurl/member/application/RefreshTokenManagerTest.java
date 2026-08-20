package com.hugo.tinyurl.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.MySqlTestcontainersConfiguration;
import com.hugo.tinyurl.RedisTestcontainersConfiguration;
import com.hugo.tinyurl.ZookeeperTestcontainersConfiguration;
import com.hugo.tinyurl.member.port.RefreshTokenRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import java.time.Duration;
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
class RefreshTokenManagerTest {

    private static final Long MEMBER_ID = 1L;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    RefreshTokenManager refreshTokenManager;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteByMemberId(MEMBER_ID);
    }

    @Test
    void rotatesTokenIssuedForMember() {
        refreshTokenManager.issue(MEMBER_ID, "token-a", Duration.ofDays(1));

        refreshTokenManager.rotate(MEMBER_ID, "token-a", "token-b", Duration.ofDays(1));

        assertThat(refreshTokenRepository.findByMemberId(MEMBER_ID)).contains("token-b");
    }

    @Test
    void rejectsRotateWithMismatchedToken() {
        refreshTokenManager.issue(MEMBER_ID, "token-a", Duration.ofDays(1));

        assertThatThrownBy(() -> refreshTokenManager.rotate(MEMBER_ID, "token-b", "token-c", Duration.ofDays(1)))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(refreshTokenRepository.findByMemberId(MEMBER_ID)).contains("token-a");
    }

    @Test
    void rejectsRotateWhenNoTokenIssued() {
        assertThatThrownBy(() -> refreshTokenManager.rotate(MEMBER_ID, "token-a", "token-b", Duration.ofDays(1)))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void revokedTokenCanNoLongerBeRotated() {
        refreshTokenManager.issue(MEMBER_ID, "token-a", Duration.ofDays(1));

        refreshTokenManager.revoke(MEMBER_ID);

        assertThatThrownBy(() -> refreshTokenManager.rotate(MEMBER_ID, "token-a", "token-b", Duration.ofDays(1)))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void issuingNewTokenReplacesPreviousOne() {
        refreshTokenManager.issue(MEMBER_ID, "token-a", Duration.ofDays(1));

        refreshTokenManager.issue(MEMBER_ID, "token-b", Duration.ofDays(1));

        assertThat(refreshTokenRepository.findByMemberId(MEMBER_ID)).contains("token-b");
        assertThatThrownBy(() -> refreshTokenManager.rotate(MEMBER_ID, "token-a", "token-c", Duration.ofDays(1)))
            .isInstanceOf(BusinessException.class);
    }

}
