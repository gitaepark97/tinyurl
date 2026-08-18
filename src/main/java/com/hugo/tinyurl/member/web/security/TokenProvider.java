package com.hugo.tinyurl.member.web.security;

import com.hugo.tinyurl.common.model.Role;
import com.hugo.tinyurl.common.web.security.AuthenticatedMember;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14);
    private static final String TYPE_CLAIM = "type";
    private static final String ROLE_CLAIM = "role";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final SecretKey key;

    TokenProvider(@Value("${app.security.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(Long memberId, Role role) {
        return generate(memberId, role, ACCESS_TYPE, ACCESS_TOKEN_EXPIRATION);
    }

    public String generateRefreshToken(Long memberId, Role role) {
        return generate(memberId, role, REFRESH_TYPE, REFRESH_TOKEN_EXPIRATION);
    }

    public Duration refreshTokenExpiration() {
        return REFRESH_TOKEN_EXPIRATION;
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return ACCESS_TYPE.equals(claims.get(TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return REFRESH_TYPE.equals(claims.get(TYPE_CLAIM, String.class));
    }

    public AuthenticatedMember toAuthenticatedMember(Claims claims) {
        return new AuthenticatedMember(Long.valueOf(claims.getSubject()), Role.valueOf(claims.get(ROLE_CLAIM, String.class)));
    }

    private String generate(Long memberId, Role role, String type, Duration expiration) {
        Date now = new Date();
        return Jwts.builder()
            .subject(memberId.toString())
            .claim(ROLE_CLAIM, role.name())
            .claim(TYPE_CLAIM, type)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiration.toMillis()))
            .signWith(key)
            .compact();
    }

}
