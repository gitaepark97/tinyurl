package com.hugo.tinyurl.common.web.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class JsonErrorResponseWriterTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void writesStatusAndUtf8EncodedBodyFromErrorCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        JsonErrorResponseWriter.write(response, ErrorCode.TOO_MANY_REQUESTS, objectMapper);

        assertThat(response.getStatus()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS.status());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        // 응답 인코딩에 기대지 않고 바이트를 직접 UTF-8로 디코딩해서 검증한다.
        String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(body)
            .contains(ErrorCode.TOO_MANY_REQUESTS.code())
            .contains(ErrorCode.TOO_MANY_REQUESTS.message());
    }

}
