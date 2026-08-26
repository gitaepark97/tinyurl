package com.hugo.tinyurl.common.web.util;

import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

public final class JsonErrorResponseWriter {

    private JsonErrorResponseWriter() {
    }

    // getWriter()는 응답 기본 인코딩(UTF-8 아님)을 따라가 한글이 깨지므로 OutputStream에 UTF-8 바이트로 직접 쓴다.
    public static void write(HttpServletResponse response, ErrorCode errorCode, ObjectMapper objectMapper) throws IOException {
        response.setStatus(errorCode.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(objectMapper.writeValueAsBytes(ApiResponse.error(errorCode.code(), errorCode.message())));
    }

}
