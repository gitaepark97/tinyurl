package com.hugo.tinyurl.member.web;

import com.hugo.tinyurl.common.web.security.AuthenticatedMember;
import com.hugo.tinyurl.member.application.MemberService;
import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.model.Role;
import com.hugo.tinyurl.member.web.request.LoginRequest;
import com.hugo.tinyurl.member.web.request.RefreshRequest;
import com.hugo.tinyurl.member.web.request.SignupRequest;
import com.hugo.tinyurl.member.web.response.MemberResponse;
import com.hugo.tinyurl.member.web.response.TokenResponse;
import com.hugo.tinyurl.member.web.security.TokenProvider;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.response.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AuthController {

    private final MemberService memberService;
    private final TokenProvider tokenProvider;

    @PostMapping("/api/v1/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        Member member = memberService.register(request.email(), request.password());
        return ApiResponse.success(MemberResponse.from(member));
    }

    @PostMapping("/api/v1/auth/login")
    ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Member member = memberService.authenticate(request.email(), request.password());
        TokenResponse tokens = generateTokens(member.id(), member.role());
        memberService.issueRefreshToken(member.id(), tokens.refreshToken(), tokenProvider.refreshTokenExpiration());
        return ApiResponse.success(tokens);
    }

    @PostMapping("/api/v1/auth/refresh")
    ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        Claims claims = parseRefreshToken(request.refreshToken());
        Long memberId = tokenProvider.toAuthenticatedMember(claims).memberId();
        // 회전 토큰의 role은 신뢰하지 않는다 — 발급 이후 권한이 바뀌었을 수 있으므로 현재 값을 다시 조회한다.
        Member member = memberService.findById(memberId);

        TokenResponse tokens = generateTokens(member.id(), member.role());
        memberService.rotateRefreshToken(
            member.id(),
            request.refreshToken(),
            tokens.refreshToken(),
            tokenProvider.refreshTokenExpiration()
        );
        return ApiResponse.success(tokens);
    }

    @PostMapping("/api/v1/auth/logout")
    ApiResponse<Void> logout(Authentication authentication) {
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        memberService.revokeRefreshToken(authenticatedMember.memberId());
        return ApiResponse.success();
    }

    private TokenResponse generateTokens(Long memberId, Role role) {
        String accessToken = tokenProvider.generateAccessToken(memberId, role);
        String refreshToken = tokenProvider.generateRefreshToken(memberId, role);
        return new TokenResponse(accessToken, refreshToken);
    }

    private Claims parseRefreshToken(String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parse(refreshToken);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, e);
        }
        if (!tokenProvider.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return claims;
    }

}
