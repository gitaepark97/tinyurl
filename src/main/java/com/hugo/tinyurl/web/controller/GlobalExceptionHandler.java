package com.hugo.tinyurl.web.controller;

import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.response.ApiResponse;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Log4j2
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.errorCode();
        if (errorCode == ErrorCode.INTERNAL_SERVER_ERROR) {
            log.error("Business exception", e);
        }
        return ResponseEntity.status(errorCode.status())
            .body(ApiResponse.error(errorCode.code(), errorCode.message()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException e
    ) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
            .body(ApiResponse.error(ErrorCode.INVALID_INPUT.code(), errors, ErrorCode.INVALID_INPUT.message()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.status())
            .body(ApiResponse.error(ErrorCode.NOT_FOUND.code(), ErrorCode.NOT_FOUND.message()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException e
    ) {
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.status())
            .headers(e.getHeaders())
            .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED.code(), ErrorCode.METHOD_NOT_ALLOWED.message()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.status())
            .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.code(), ErrorCode.INTERNAL_SERVER_ERROR.message()));
    }

}
