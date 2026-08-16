package com.hugo.tinyurl.member.port;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(Long memberId, String refreshToken, Duration ttl);

    Optional<String> findByMemberId(Long memberId);

    boolean replace(Long memberId, String expectedRefreshToken, String newRefreshToken, Duration ttl);

    void deleteByMemberId(Long memberId);

}
