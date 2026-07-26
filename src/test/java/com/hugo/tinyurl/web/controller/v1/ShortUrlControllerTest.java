package com.hugo.tinyurl.web.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugo.tinyurl.domain.entity.ShortUrl;
import com.hugo.tinyurl.domain.service.ShortUrlService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShortUrlController.class)
@AutoConfigureRestDocs
class ShortUrlControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ShortUrlService shortUrlService;

    @Test
    void createsShortUrl() throws Exception {
        ShortUrl shortUrl = new ShortUrl("abc12345", "https://example.com", LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS));
        given(shortUrlService.create(any())).willReturn(shortUrl);

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalUrl\":\"https://example.com\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.shortKey").value("abc12345"))
            .andDo(document("short-url-create",
                requestFields(fieldWithPath("originalUrl").description("단축할 원본 URL")),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.shortKey").description("발급된 단축 키"),
                    fieldWithPath("data.shortUrl").description("단축 URL 전체 주소"),
                    fieldWithPath("data.expiresAt").description("만료 일시"),
                    fieldWithPath("message").description("에러 메시지(성공 시 null)")
                )));
    }

    @Test
    void rejectsInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalUrl\":\"not-a-url\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andDo(document("short-url-create-invalid",
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.originalUrl").description("필드별 검증 실패 메시지"),
                    fieldWithPath("message").description("에러 메시지")
                )));
    }

    @Test
    void rejectsUrlWithWhitespace() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalUrl\":\"https://example.com/a b\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

}
