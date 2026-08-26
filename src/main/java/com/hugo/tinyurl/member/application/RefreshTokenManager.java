package com.hugo.tinyurl.member.application;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.member.port.RefreshTokenRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Observed
@Component
@RequiredArgsConstructor
class RefreshTokenManager {

    private final RefreshTokenRepository refreshTokenRepository;

    void issue(Long memberId, String refreshToken, Duration ttl) {
        refreshTokenRepository.save(memberId, refreshToken, ttl);
    }

    void rotate(Long memberId, String oldRefreshToken, String newRefreshToken, Duration ttl) {
        boolean rotated = refreshTokenRepository.replace(memberId, oldRefreshToken, newRefreshToken, ttl);
        if (!rotated) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    void revoke(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

}
