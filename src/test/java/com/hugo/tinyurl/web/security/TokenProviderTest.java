package com.hugo.tinyurl.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.domain.model.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

class TokenProviderTest {

    private static final String SECRET = "test-only-jwt-secret-that-is-long-enough-for-hs256";

    private final TokenProvider tokenProvider = new TokenProvider(SECRET);

    @Test
    void parsesGeneratedAccessTokenBackToMemberIdAndRole() {
        String token = tokenProvider.generateAccessToken(1L, Role.MEMBER);
        Claims claims = tokenProvider.parse(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role", String.class)).isEqualTo("MEMBER");
        assertThat(tokenProvider.isAccessToken(claims)).isTrue();
        assertThat(tokenProvider.isRefreshToken(claims)).isFalse();

        AuthenticatedMember authenticatedMember = tokenProvider.toAuthenticatedMember(claims);
        assertThat(authenticatedMember.memberId()).isEqualTo(1L);
        assertThat(authenticatedMember.role()).isEqualTo(Role.MEMBER);
    }

    @Test
    void parsesGeneratedRefreshTokenBackToMemberIdAndRole() {
        String token = tokenProvider.generateRefreshToken(1L, Role.ADMIN);
        Claims claims = tokenProvider.parse(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(tokenProvider.isRefreshToken(claims)).isTrue();
        assertThat(tokenProvider.isAccessToken(claims)).isFalse();
    }

}
