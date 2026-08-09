package com.hugo.tinyurl.domain.application;

import com.hugo.tinyurl.domain.model.Member;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRegistrar memberRegistrar;
    private final MemberAuthenticator memberAuthenticator;
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
