package com.hugo.tinyurl.web.controller.v1.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ShortUrlCreateRequest(
    @NotBlank
    @Size(max = 2048, message = "URL은 2048자를 넘을 수 없습니다")
    @Pattern(regexp = "^https?://\\S+$", message = "URL은 http:// 또는 https://로 시작해야 하고 공백을 포함할 수 없습니다")
    String originalUrl,

    // 리다이렉트 라우트(/{shortKey:[0-9A-Za-z]{1,8}})와 동일한 문자 집합/길이 제약을 따른다.
    @Pattern(regexp = "^[0-9A-Za-z]{1,8}$", message = "customAlias는 1~8자의 영숫자여야 합니다")
    String customAlias,

    @Future(message = "expiresAt은 미래 시각이어야 합니다")
    LocalDateTime expiresAt
) {
}
