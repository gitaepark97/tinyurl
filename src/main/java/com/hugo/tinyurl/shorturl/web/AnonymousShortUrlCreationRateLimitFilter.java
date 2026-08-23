package com.hugo.tinyurl.shorturl.web;

import com.hugo.tinyurl.common.web.security.AuthenticatedMember;
import com.hugo.tinyurl.common.web.util.ClientIpResolver;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.response.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TimeoutException;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

// @Component로 등록하지 않는다 - Filter 빈은 @WebMvcTest에도 자동 포함되는데, shorturl 전용 설정에 의존해 슬라이스 테스트가 깨진다.
@Slf4j
class AnonymousShortUrlCreationRateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<byte[]> proxyManager;
    private final ObjectMapper objectMapper;
    private final BucketConfiguration bucketConfiguration;

    AnonymousShortUrlCreationRateLimitFilter(
        ProxyManager<byte[]> proxyManager,
        ObjectMapper objectMapper,
        long capacity,
        long refillTokens,
        long refillDurationSeconds
    ) {
        this.proxyManager = proxyManager;
        this.objectMapper = objectMapper;
        this.bucketConfiguration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofSeconds(refillDurationSeconds))
                .build())
            .build();
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!"POST".equals(request.getMethod())
            || AuthenticatedMember.memberIdOrNull(SecurityContextHolder.getContext().getAuthentication()) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (tryConsume(request)) {
            filterChain.doFilter(request, response);
        } else {
            writeTooManyRequests(response);
        }
    }

    private boolean tryConsume(HttpServletRequest request) {
        try {
            String ip = ClientIpResolver.resolve(request);
            Bucket bucket = proxyManager.getProxy(ip.getBytes(StandardCharsets.UTF_8), () -> bucketConfiguration);
            return bucket.tryConsume(1);
        } catch (RedisException | TimeoutException e) {
            // rate limit은 보호 장치일 뿐이라 Redis 장애/타임아웃 시엔 fail-open - 다른 예외까지 삼키면 안 돼 넓게 잡지 않는다.
            log.warn("Rate limit 확인 실패 - 요청을 통과시킨다", e);
            return true;
        }
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;
        response.setStatus(errorCode.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            objectMapper.writeValueAsString(ApiResponse.error(errorCode.code(), errorCode.message())));
    }

}
