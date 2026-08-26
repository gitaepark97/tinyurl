package com.hugo.tinyurl.common.web.response;

public record ApiResponse<T>(
    String code,
    T data,
    String message
) {

    private static final String SUCCESS_CODE = "SUCCESS";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(SUCCESS_CODE, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, null, message);
    }

    public static <T> ApiResponse<T> error(String code, T data, String message) {
        return new ApiResponse<>(code, data, message);
    }

}
