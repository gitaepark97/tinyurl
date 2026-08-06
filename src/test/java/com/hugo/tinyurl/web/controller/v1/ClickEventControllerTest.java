package com.hugo.tinyurl.web.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugo.tinyurl.domain.entity.ClickEvent;
import com.hugo.tinyurl.domain.application.ClickEventService;
import com.hugo.tinyurl.support.page.Page;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ClickEventController.class)
@AutoConfigureRestDocs
class ClickEventControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ClickEventService clickEventService;

    @Test
    void findsClickEventList() throws Exception {
        ClickEvent event = newClickEvent(1L, "127.0.0.1", "test-agent", "https://referer.example.com");
        given(clickEventService.findAll(eq(1L), any())).willReturn(Page.of(List.of(event), true));

        mockMvc.perform(get("/api/v1/urls/{id}/click-events", 1L)
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
    void returnsEmptyPageForUnknownShortUrlId() throws Exception {
        given(clickEventService.findAll(eq(999L), any())).willReturn(Page.of(List.of(), false));

        mockMvc.perform(get("/api/v1/urls/{id}/click-events", 999L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void rejectsOutOfRangeSize() throws Exception {
        mockMvc.perform(get("/api/v1/urls/{id}/click-events", 1L).param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private ClickEvent newClickEvent(Long id, String ipAddress, String userAgent, String referer) {
        ClickEvent event = new ClickEvent(1L, ipAddress, userAgent, referer);
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

}
