package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.Member;
import com.hugo.tinyurl.domain.model.Role;
import io.micrometer.observation.annotation.Observed;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Observed
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRegistrar memberRegistrar;
    private final MemberAuthenticator memberAuthenticator;
    private final MemberRoleUpdater memberRoleUpdater;
    private final RefreshTokenManager refreshTokenManager;

    public Member register(String email, String rawPassword) {
        return memberRegistrar.register(email, rawPassword);
    }

    public Member authenticate(String email, String rawPassword) {
        return memberAuthenticator.authenticate(email, rawPassword);
    }

    public Member findById(Long id) {
        return memberAuthenticator.findById(id);
    }

    public Member updateRole(Long id, Role role) {
        return memberRoleUpdater.updateRole(id, role);
    }

    public void issueRefreshToken(Long memberId, String refreshToken, Duration ttl) {
        refreshTokenManager.issue(memberId, refreshToken, ttl);
    }

    public void rotateRefreshToken(Long memberId, String oldRefreshToken, String newRefreshToken, Duration ttl) {
        refreshTokenManager.rotate(memberId, oldRefreshToken, newRefreshToken, ttl);
    }

    public void revokeRefreshToken(Long memberId) {
        refreshTokenManager.revoke(memberId);
    }

}
