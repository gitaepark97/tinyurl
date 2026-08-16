package com.hugo.tinyurl.member.web.response;

public record TokenResponse(
    String accessToken,
    String refreshToken
) {
}
