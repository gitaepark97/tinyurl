package com.hugo.tinyurl.shorturl.web;

import com.hugo.tinyurl.member.model.Role;
import com.hugo.tinyurl.member.web.security.AuthenticatedMember;
import com.hugo.tinyurl.shorturl.application.ShortUrlService;
import com.hugo.tinyurl.shorturl.model.ShortUrl;
import com.hugo.tinyurl.shorturl.model.ShortUrlWithClickCount;
import com.hugo.tinyurl.shorturl.web.request.ShortUrlCreateRequest;
import com.hugo.tinyurl.shorturl.web.response.ShortUrlResponse;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import com.hugo.tinyurl.support.response.ApiResponse;
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
        Long memberId = AuthenticatedMember.memberIdOrNull(authentication);
        ShortUrl shortUrl = shortUrlService.create(memberId, request.originalUrl(), request.customAlias(), request.expiresAt());
        return ApiResponse.success(ShortUrlResponse.from(ShortUrlWithClickCount.of(shortUrl, 0L), baseUrl));
    }

    @GetMapping("/api/v1/urls")
    ApiResponse<Page<ShortUrlResponse>> findAll(@ModelAttribute PageParam pageParam) {
        Page<ShortUrlWithClickCount> page = shortUrlService.findAll(pageParam);
        return ApiResponse.success(page.map(view -> ShortUrlResponse.from(view, baseUrl)));
    }

    @GetMapping("/api/v1/urls/me")
    ApiResponse<Page<ShortUrlResponse>> findMine(@ModelAttribute PageParam pageParam, Authentication authentication) {
        AuthenticatedMember authenticatedMember = AuthenticatedMember.from(authentication);
        if (authenticatedMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Page<ShortUrlWithClickCount> page = shortUrlService.findAllByMember(authenticatedMember.memberId(), pageParam);
        return ApiResponse.success(page.map(view -> ShortUrlResponse.from(view, baseUrl)));
    }

    @GetMapping("/api/v1/urls/{id}")
    ApiResponse<ShortUrlResponse> find(@PathVariable Long id, Authentication authentication) {
        Long requesterMemberId = AuthenticatedMember.memberIdOrNull(authentication);
        Role requesterRole = AuthenticatedMember.roleOrNull(authentication);
        ShortUrlWithClickCount view = shortUrlService.find(id, requesterMemberId, requesterRole);
        return ApiResponse.success(ShortUrlResponse.from(view, baseUrl));
    }

}
