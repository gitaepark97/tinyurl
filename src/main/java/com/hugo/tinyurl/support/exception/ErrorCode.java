package com.hugo.tinyurl.support.exception;

public enum ErrorCode {

    INVALID_INPUT(400, "INVALID_INPUT", "잘못된 요청입니다"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다"),
    FORBIDDEN(403, "FORBIDDEN", "권한이 없습니다"),
    NOT_FOUND(404, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다"),
    CONFLICT(409, "CONFLICT", "이미 사용 중입니다"),
    LAST_ADMIN_DEMOTION(409, "LAST_ADMIN_DEMOTION", "마지막 관리자는 강등할 수 없습니다"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

}
