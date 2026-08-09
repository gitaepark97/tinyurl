package com.hugo.tinyurl.web.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugo.tinyurl.domain.application.ClickEventService;
import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.domain.model.Role;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.web.security.ApiAccessDeniedHandler;
import com.hugo.tinyurl.web.security.ApiAuthenticationEntryPoint;
import com.hugo.tinyurl.web.security.AuthenticatedMember;
import com.hugo.tinyurl.web.security.SecurityConfig;
import com.hugo.tinyurl.web.security.TokenProvider;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 소유자/관리자 여부로 분기하는 로직을 검증해야 해서 실제 SecurityConfig를 끌어와 필터 체인을 적용한다.
@WebMvcTest(controllers = ClickEventController.class)
@Import({SecurityConfig.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
@AutoConfigureRestDocs
class ClickEventControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ClickEventService clickEventService;

    @MockitoBean
    TokenProvider tokenProvider;

    @Test
    void findsOwnClickEventList() throws Exception {
        ClickEvent event = newClickEvent(1L, "127.0.0.1", "test-agent", "https://referer.example.com");
        given(clickEventService.findAll(eq(1L), eq(10L), eq(Role.MEMBER), any())).willReturn(Page.of(List.of(event), true));
        stubAuthenticatedMember("member-access-token", new AuthenticatedMember(10L, Role.MEMBER));

        mockMvc.perform(get("/api/v1/urls/{id}/click-events", 1L)
                .header("Authorization", "Bearer member-access-token")
                .param("cursor", "10")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].id").value(1))
            .andExpect(jsonPath("$.data.hasNext").value(true))
            .andDo(document("click-event-list",
                pathParameters(parameterWithName("id").description("단축 URL id")),
                queryParameters(
                    parameterWithName("cursor").description("이전 페이지 마지막 항목의 id(생략 시 최신부터 조회)").optional(),
                    parameterWithName("size").description("페이지 크기(1~100, 기본값 20)").optional()
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.content[].id").description("클릭 이벤트 id"),
                    fieldWithPath("data.content[].ipAddress").description("클릭 발생 IP"),
                    fieldWithPath("data.content[].userAgent").description("User-Agent"),
                    fieldWithPath("data.content[].referer").description("Referer"),
                    fieldWithPath("data.content[].clickedAt").description("클릭 일시"),
                    fieldWithPath("data.hasNext").description("다음 페이지 존재 여부"),
                    fieldWithPath("message").description("에러 메시지(성공 시 null)")
                )));
    }

    @Test
    void allowsAdminToViewAnyClickEventList() throws Exception {
        ClickEvent event = newClickEvent(1L, "127.0.0.1", "test-agent", null);
        given(clickEventService.findAll(eq(1L), eq(99L), eq(Role.ADMIN), any())).willReturn(Page.of(List.of(event), false));
        stubAuthenticatedMember("admin-access-token", new AuthenticatedMember(99L, Role.ADMIN));

        mockMvc.perform(get("/api/v1/urls/{id}/click-events", 1L).header("Authorization", "Bearer admin-access-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void rejectsClickEventListFromNonOwner() throws Exception {
        given(clickEventService.findAll(eq(1L), eq(20L), eq(Role.MEMBER), any()))
            .willThrow(new BusinessException(ErrorCode.FORBIDDEN));
        stubAuthenticatedMember("member-access-token", new AuthenticatedMember(20L, Role.MEMBER));

        mockMvc.perform(get("/api/v1/urls/{id}/click-events", 1L).header("Authorization", "Bearer member-access-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void returnsNotFoundForUnknownShortUrlId() throws Exception {
        given(clickEventService.findAll(eq(999L), any(), any(), any()))
            .willThrow(new BusinessException(ErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/v1/urls/{id}/click-events", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void rejectsOutOfRangeSize() throws Exception {
        mockMvc.perform(get("/api/v1/urls/{id}/click-events", 1L).param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private void stubAuthenticatedMember(String token, AuthenticatedMember authenticatedMember) {
        Claims claims = mock(Claims.class);
        given(tokenProvider.parse(token)).willReturn(claims);
        given(tokenProvider.isAccessToken(claims)).willReturn(true);
        given(tokenProvider.toAuthenticatedMember(claims)).willReturn(authenticatedMember);
    }

    private ClickEvent newClickEvent(Long id, String ipAddress, String userAgent, String referer) {
        return new ClickEvent(id, 1L, ipAddress, userAgent, referer, LocalDateTime.now());
    }

}
