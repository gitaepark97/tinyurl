package com.hugo.tinyurl.shorturl.web;

import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.web.security.AuthenticatedMember;
import com.hugo.tinyurl.common.web.util.JsonErrorResponseWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

// @Component로 등록하지 않는다 - Filter 빈은 @WebMvcTest에도 자동 포함되는데, shorturl 전용 설정에 의존해 슬라이스 테스트가 깨진다.
class MemberShortUrlCreationRateLimitFilter extends OncePerRequestFilter {

    // 익명 필터의 IP 키(접두어 없음)와 네임스페이스가 섞이지 않도록 접두어를 붙인다.
    private static final String BUCKET_KEY_PREFIX = "member:";

    private final ProxyManager<byte[]> proxyManager;
    private final ObjectMapper objectMapper;
    private final BucketConfiguration bucketConfiguration;

    MemberShortUrlCreationRateLimitFilter(
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
        Long memberId = AuthenticatedMember.memberIdOrNull(SecurityContextHolder.getContext().getAuthentication());
        if (!"POST".equals(request.getMethod()) || memberId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (tryConsume(memberId)) {
            filterChain.doFilter(request, response);
        } else {
            JsonErrorResponseWriter.write(response, ErrorCode.TOO_MANY_REQUESTS, objectMapper);
        }
    }

    private boolean tryConsume(Long memberId) {
        String key = BUCKET_KEY_PREFIX + memberId;
        return RateLimitBucketConsumer.tryConsume(proxyManager, key, bucketConfiguration);
    }

}
