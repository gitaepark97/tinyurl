package com.hugo.tinyurl.web.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugo.tinyurl.domain.application.ShortUrlService;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.model.ShortUrlWithClickCount;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.web.security.TokenProvider;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 이 컨트롤러는 인가 규칙을 다루지 않으므로 보안 필터 체인을 아예 적용하지 않는다 - 적용하면
// 커스텀 SecurityConfig가 없는 이 슬라이스에서 Spring Boot 기본 보안 설정(CSRF 등)으로 대체돼
// 무관한 요청까지 막힌다.
@WebMvcTest(controllers = ShortUrlController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class ShortUrlControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ShortUrlService shortUrlService;

    // JwtAuthenticationFilter(Filter 타입)는 @WebMvcTest가 그대로 빈으로 끌어온다 - addFilters=false로
    // 실행은 막아도 빈 생성 자체엔 TokenProvider가 필요하다.
    @MockitoBean
    TokenProvider tokenProvider;

    @Test
    void createsShortUrl() throws Exception {
        ShortUrl shortUrl = newShortUrl(1L, "abc12345", "https://example.com");
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
                    fieldWithPath("data.id").description("단축 URL id"),
                    fieldWithPath("data.shortKey").description("발급된 단축 키"),
                    fieldWithPath("data.shortUrl").description("단축 URL 전체 주소"),
                    fieldWithPath("data.originalUrl").description("원본 URL"),
                    fieldWithPath("data.clickCount").description("누적 클릭 수"),
                    fieldWithPath("data.expiresAt").description("만료 일시"),
                    fieldWithPath("data.createdAt").description("생성 일시"),
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

    @Test
    void findsShortUrlList() throws Exception {
        ShortUrl shortUrl = newShortUrl(1L, "abc12345", "https://example.com");
        given(shortUrlService.findAll(any())).willReturn(Page.of(List.of(ShortUrlWithClickCount.of(shortUrl, 3L)), true));

        mockMvc.perform(get("/api/v1/urls")
                .param("cursor", "10")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].shortKey").value("abc12345"))
            .andExpect(jsonPath("$.data.content[0].clickCount").value(3))
            .andExpect(jsonPath("$.data.hasNext").value(true))
            .andDo(document("short-url-list",
                queryParameters(
                    parameterWithName("cursor").description("이전 페이지 마지막 항목의 id(생략 시 최신부터 조회)").optional(),
                    parameterWithName("size").description("페이지 크기(1~100, 기본값 20)").optional()
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.content[].id").description("단축 URL id"),
                    fieldWithPath("data.content[].shortKey").description("단축 키"),
                    fieldWithPath("data.content[].shortUrl").description("단축 URL 전체 주소"),
                    fieldWithPath("data.content[].originalUrl").description("원본 URL"),
                    fieldWithPath("data.content[].clickCount").description("누적 클릭 수"),
                    fieldWithPath("data.content[].expiresAt").description("만료 일시"),
                    fieldWithPath("data.content[].createdAt").description("생성 일시"),
                    fieldWithPath("data.hasNext").description("다음 페이지 존재 여부"),
                    fieldWithPath("message").description("에러 메시지(성공 시 null)")
                )));
    }

    @Test
    void rejectsOutOfRangeSize() throws Exception {
        mockMvc.perform(get("/api/v1/urls").param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void findsShortUrlById() throws Exception {
        ShortUrl shortUrl = newShortUrl(1L, "abc12345", "https://example.com");
        given(shortUrlService.find(1L)).willReturn(ShortUrlWithClickCount.of(shortUrl, 3L));

        mockMvc.perform(get("/api/v1/urls/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.shortKey").value("abc12345"))
            .andExpect(jsonPath("$.data.clickCount").value(3))
            .andDo(document("short-url-find",
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.id").description("단축 URL id"),
                    fieldWithPath("data.shortKey").description("단축 키"),
                    fieldWithPath("data.shortUrl").description("단축 URL 전체 주소"),
                    fieldWithPath("data.originalUrl").description("원본 URL"),
                    fieldWithPath("data.clickCount").description("누적 클릭 수"),
                    fieldWithPath("data.expiresAt").description("만료 일시"),
                    fieldWithPath("data.createdAt").description("생성 일시"),
                    fieldWithPath("message").description("에러 메시지(성공 시 null)")
                )));
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        given(shortUrlService.find(eq(999L))).willThrow(new BusinessException(ErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/v1/urls/{id}", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andDo(document("short-url-find-not-found"));
    }

    private ShortUrl newShortUrl(Long id, String shortKey, String originalUrl) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return new ShortUrl(id, shortKey, originalUrl, now.plusDays(7), now);
    }

}
