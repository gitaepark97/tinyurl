package com.hugo.tinyurl.web.controller;

import com.hugo.tinyurl.domain.service.ShortUrlService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ShortUrlRedirectController {

    private final ShortUrlService shortUrlService;

    @GetMapping("/{shortKey:[0-9A-Za-z]{8}}")
    ResponseEntity<Void> redirect(@PathVariable String shortKey) {
        String originalUrl = shortUrlService.getOriginalUrl(shortKey);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build();
    }

}
