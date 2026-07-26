package com.hugo.tinyurl.web.controller.v1;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.service.ShortUrlService;
import com.hugo.tinyurl.support.response.ApiResponse;
import com.hugo.tinyurl.web.controller.v1.request.ShortUrlCreateRequest;
import com.hugo.tinyurl.web.controller.v1.response.ShortUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
    ApiResponse<ShortUrlResponse> create(@Valid @RequestBody ShortUrlCreateRequest request) {
        ShortUrl shortUrl = shortUrlService.create(request.originalUrl());
        return ApiResponse.success(ShortUrlResponse.from(shortUrl, baseUrl));
    }

}
