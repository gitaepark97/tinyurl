package com.hugo.tinyurl.member.infra.cache;

import com.hugo.tinyurl.member.port.RefreshTokenRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

// ShortUrlCacheRepository와 달리 Redis 장애를 흡수하지 않는다 — 세션 유효성의 근거(source of
// truth)라 장애 시 예외를 그대로 전파해 500으로 응답하는 게 맞다.
@Component
class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh-token:";

    // GET 후 별도 SET을 하면 동시 요청이 검증을 동시에 통과하는 race가 생겨 Lua로 원자적 compare-and-set을 수행한다.
    private static final RedisScript<Long> REPLACE_SCRIPT = new DefaultRedisScript<>(
        "if redis.call('GET', KEYS[1]) == ARGV[1] then "
            + "redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]) return 1 "
            + "else return 0 end",
        Long.class
    );

    private final StringRedisTemplate redisTemplate;

    RedisRefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(Long memberId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(memberId), refreshToken, ttl);
    }

    @Override
    public Optional<String> findByMemberId(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(memberId)));
    }

    @Override
    public boolean replace(Long memberId, String expectedRefreshToken, String newRefreshToken, Duration ttl) {
        Long replaced = redisTemplate.execute(
            REPLACE_SCRIPT, List.of(key(memberId)), expectedRefreshToken, newRefreshToken, String.valueOf(ttl.toMillis()));
        return Long.valueOf(1L).equals(replaced);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }

}
