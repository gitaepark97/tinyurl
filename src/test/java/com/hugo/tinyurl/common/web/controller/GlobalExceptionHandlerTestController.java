package com.hugo.tinyurl.common.web.controller;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.page.PageParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/test/type-mismatch/{id}")
    void typeMismatch(@PathVariable Long id) {
    }

    @GetMapping("/test/model-attribute")
    void modelAttribute(@ModelAttribute PageParam pageParam) {
    }

    @GetMapping("/test/unknown-exception")
    void throwUnknownException() {
        throw new IllegalStateException("boom");
    }

    record TestRequest(@NotBlank String name) {
    }

}
