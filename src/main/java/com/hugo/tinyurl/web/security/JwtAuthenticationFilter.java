package com.hugo.tinyurl.web.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 토큰이 없거나 유효하지 않아도 요청을 막지 않는다 — 강제는 SecurityConfig가 경로별로 담당한다.
@Slf4j
@Component
@RequiredArgsConstructor
class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                authenticate(token);
            } catch (RuntimeException e) {
                // 서명 오류(JwtException) 외에도 claim이 기대한 형식이 아니면 IllegalArgumentException/NPE가 날 수 있다.
                log.debug("JWT 검증 실패", e);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void authenticate(String token) {
        Claims claims = tokenProvider.parse(token);
        if (!tokenProvider.isAccessToken(claims)) {
            return;
        }

        AuthenticatedMember authenticatedMember = tokenProvider.toAuthenticatedMember(claims);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + authenticatedMember.role().name()));

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(authenticatedMember, null, authorities));
    }

}
