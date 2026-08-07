package com.hugo.tinyurl.web.controller.v1;

import com.hugo.tinyurl.domain.application.ShortUrlService;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.model.ShortUrlWithClickCount;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import com.hugo.tinyurl.support.response.ApiResponse;
import com.hugo.tinyurl.web.controller.v1.request.ShortUrlCreateRequest;
import com.hugo.tinyurl.web.controller.v1.response.ShortUrlResponse;
import com.hugo.tinyurl.web.security.AuthenticatedMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ShortUrlController {

    private final ShortUrlService shortUrlService;

    @Value("${app.short-url.base-url}")
    private String baseUrl;

    @PostMapping("/api/v1/urls")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ShortUrlResponse> create(@Valid @RequestBody ShortUrlCreateRequest request, Authentication authentication) {
        ShortUrl shortUrl = shortUrlService.create(
            memberId(authentication), request.originalUrl(), request.customAlias(), request.expiresAt());
        return ApiResponse.success(ShortUrlResponse.from(ShortUrlWithClickCount.of(shortUrl, 0L), baseUrl));
    }

    @GetMapping("/api/v1/urls")
    ApiResponse<Page<ShortUrlResponse>> findAll(@ModelAttribute PageParam pageParam) {
        Page<ShortUrlWithClickCount> page = shortUrlService.findAll(pageParam);
        return ApiResponse.success(page.map(view -> ShortUrlResponse.from(view, baseUrl)));
    }

    @GetMapping("/api/v1/urls/{id}")
    ApiResponse<ShortUrlResponse> find(@PathVariable Long id) {
        ShortUrlWithClickCount view = shortUrlService.find(id);
        return ApiResponse.success(ShortUrlResponse.from(view, baseUrl));
    }

    private Long memberId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember) {
            return authenticatedMember.memberId();
        }
        return null;
    }

}
