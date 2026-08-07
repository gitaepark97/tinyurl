package com.hugo.tinyurl.web.controller.v1.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank
    String refreshToken
) {
}
