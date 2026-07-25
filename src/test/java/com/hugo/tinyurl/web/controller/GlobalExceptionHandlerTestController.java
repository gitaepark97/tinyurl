package com.hugo.tinyurl.web.controller;

import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class GlobalExceptionHandlerTestController {

    @GetMapping("/test/business-exception")
    void throwBusinessException() {
        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    @PostMapping("/test/validation")
    void validate(@RequestBody @Valid TestRequest request) {
    }

    @GetMapping("/test/unknown-exception")
    void throwUnknownException() {
        throw new IllegalStateException("boom");
    }

    record TestRequest(@NotBlank String name) {
    }

}
