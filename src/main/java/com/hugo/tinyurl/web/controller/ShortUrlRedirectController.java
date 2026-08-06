package com.hugo.tinyurl.web.controller;

import com.hugo.tinyurl.domain.application.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ShortUrlRedirectController {

    private final ShortUrlService shortUrlService;

    @GetMapping("/{shortKey:[0-9A-Za-z]{8}}")
    ResponseEntity<Void> redirect(
        @PathVariable String shortKey,
        @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
        @RequestHeader(value = HttpHeaders.REFERER, required = false) String referer,
        HttpServletRequest request
    ) {
        String originalUrl = shortUrlService.redirect(shortKey, request.getRemoteAddr(), userAgent, referer);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build();
    }

}
