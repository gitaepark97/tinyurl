package com.hugo.tinyurl.shorturl.web;

import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.web.security.AuthenticatedMember;
import com.hugo.tinyurl.common.web.util.ClientIpResolver;
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
            JsonErrorResponseWriter.write(response, ErrorCode.TOO_MANY_REQUESTS, objectMapper);
        }
    }

    private boolean tryConsume(HttpServletRequest request) {
        String ip = ClientIpResolver.resolve(request);
        return RateLimitBucketConsumer.tryConsume(proxyManager, ip, bucketConfiguration);
    }

}
