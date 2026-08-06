package com.hugo.tinyurl.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugo.tinyurl.domain.application.ShortUrlService;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShortUrlRedirectController.class)
@AutoConfigureRestDocs
class ShortUrlRedirectControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ShortUrlService shortUrlService;

    @Test
    void redirectsToOriginalUrl() throws Exception {
        given(shortUrlService.redirect(eq("abc12345"), any(), any(), any())).willReturn("https://example.com");

        mockMvc.perform(get("/{shortKey}", "abc12345").header("User-Agent", "test-agent"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://example.com"))
            .andDo(document("short-url-redirect"));

        verify(shortUrlService).redirect(eq("abc12345"), any(), eq("test-agent"), any());
    }

    @Test
    void returnsNotFoundForUnknownKey() throws Exception {
        given(shortUrlService.redirect(eq("nope0000"), any(), any(), any()))
            .willThrow(new BusinessException(ErrorCode.NOT_FOUND));

        mockMvc.perform(get("/{shortKey}", "nope0000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andDo(document("short-url-redirect-not-found"));
    }

    @Test
    void doesNotMatchNonKeyShapedPath() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(shortUrlService);
    }

}
