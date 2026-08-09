package com.hugo.tinyurl.web.controller.v1.response;

public record TokenResponse(
    String accessToken,
    String refreshToken
) {
}
