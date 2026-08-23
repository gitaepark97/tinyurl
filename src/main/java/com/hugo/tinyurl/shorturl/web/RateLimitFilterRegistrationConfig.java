package com.hugo.tinyurl.shorturl.web;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import tools.jackson.databind.ObjectMapper;

// member의 SecurityConfig에 끼워 넣지 않고 별도 필터로 등록한다 - 그러면 member가 shorturl에 의존하게 돼 모듈 경계가 깨진다.
@Configuration
class RateLimitFilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<AnonymousShortUrlCreationRateLimitFilter> anonymousShortUrlCreationRateLimitFilterRegistration(
        @Lazy ProxyManager<byte[]> proxyManager,
        ObjectMapper objectMapper,
        @Value("${app.rate-limit.anonymous-url-creation.capacity}") long capacity,
        @Value("${app.rate-limit.anonymous-url-creation.refill-tokens}") long refillTokens,
        @Value("${app.rate-limit.anonymous-url-creation.refill-duration-seconds}") long refillDurationSeconds
    ) {
        AnonymousShortUrlCreationRateLimitFilter filter = new AnonymousShortUrlCreationRateLimitFilter(
            proxyManager, objectMapper, capacity, refillTokens, refillDurationSeconds);
        FilterRegistrationBean<AnonymousShortUrlCreationRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/v1/urls");
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1);
        return registration;
    }

}
