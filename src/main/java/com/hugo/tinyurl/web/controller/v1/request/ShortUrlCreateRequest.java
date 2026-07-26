package com.hugo.tinyurl.web.controller.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShortUrlCreateRequest(
    @NotBlank
    @Size(max = 2048, message = "URL은 2048자를 넘을 수 없습니다")
    @Pattern(regexp = "^https?://\\S+$", message = "URL은 http:// 또는 https://로 시작해야 하고 공백을 포함할 수 없습니다")
    String originalUrl
) {
}
