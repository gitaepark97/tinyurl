package com.hugo.tinyurl.member.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import com.hugo.tinyurl.common.model.Role;
import com.hugo.tinyurl.common.web.security.AuthenticatedMember;
import com.hugo.tinyurl.member.application.MemberService;
import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.web.security.ApiAccessDeniedHandler;
import com.hugo.tinyurl.member.web.security.ApiAuthenticationEntryPoint;
import com.hugo.tinyurl.member.web.security.SecurityConfig;
import com.hugo.tinyurl.member.web.security.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// SecurityConfig를 명시로 import하지 않으면 Boot 기본 보안 설정으로 대체돼 인가 규칙을 검증할 수 없다.
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
@AutoConfigureRestDocs
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    TokenProvider tokenProvider;

    @Test
    void signsUpMember() throws Exception {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Member member = new Member(1L, "user@example.com", "encoded", Role.MEMBER, now);
        given(memberService.register(any(), any())).willReturn(member);

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andDo(document("auth-signup",
                requestFields(
                    fieldWithPath("email").description("이메일"),
                    fieldWithPath("password").description("비밀번호(8~64자, 영문+숫자 포함)")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.id").description("회원 id"),
                    fieldWithPath("data.email").description("이메일"),
                    fieldWithPath("data.createdAt").description("가입 일시"),
                    fieldWithPath("message").description("에러 메시지(성공 시 null)")
                )));
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"password\":\"password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void rejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void rejectsPasswordWithoutDigit() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"onlyletters\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void rejectsPasswordWithoutLetter() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"12345678\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        given(memberService.register(any(), any())).willThrow(new BusinessException(ErrorCode.CONFLICT));

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andDo(document("auth-signup-conflict"));
    }

    @Test
    void logsInMember() throws Exception {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Member member = new Member(1L, "user@example.com", "encoded", Role.MEMBER, now);
        given(memberService.authenticate(any(), any())).willReturn(member);
        given(tokenProvider.generateAccessToken(1L, Role.MEMBER)).willReturn("issued-access-token");
        given(tokenProvider.generateRefreshToken(1L, Role.MEMBER)).willReturn("issued-refresh-token");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("issued-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("issued-refresh-token"))
            .andDo(document("auth-login",
                requestFields(
                    fieldWithPath("email").description("이메일"),
                    fieldWithPath("password").description("비밀번호")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.accessToken").description("발급된 access token"),
                    fieldWithPath("data.refreshToken").description("발급된 refresh token"),
                    fieldWithPath("message").description("에러 메시지(성공 시 null)")
                )));

        verify(memberService).issueRefreshToken(eq(1L), eq("issued-refresh-token"), any());
    }

    @Test
    void rejectsLoginWithWrongCredentials() throws Exception {
        given(memberService.authenticate(any(), any())).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andDo(document("auth-login-unauthorized"));
    }

    @Test
    void refreshesTokens() throws Exception {
        Claims claims = mock(Claims.class);
        given(tokenProvider.parse("old-refresh-token")).willReturn(claims);
        given(tokenProvider.isRefreshToken(claims)).willReturn(true);
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(1L, Role.MEMBER);
        given(tokenProvider.toAuthenticatedMember(claims)).willReturn(authenticatedMember);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        given(memberService.findById(1L)).willReturn(new Member(1L, "user@example.com", "encoded", Role.MEMBER, now));
        given(tokenProvider.generateAccessToken(1L, Role.MEMBER)).willReturn("new-access-token");
        given(tokenProvider.generateRefreshToken(1L, Role.MEMBER)).willReturn("new-refresh-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"old-refresh-token\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
            .andDo(document("auth-refresh",
                requestFields(fieldWithPath("refreshToken").description("발급받은 refresh token")),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.accessToken").description("새로 발급된 access token"),
                    fieldWithPath("data.refreshToken").description("새로 발급된 refresh token(회전됨)"),
                    fieldWithPath("message").description("에러 메시지(성공 시 null)")
                )));

        verify(memberService).rotateRefreshToken(eq(1L), eq("old-refresh-token"), eq("new-refresh-token"), any());
    }

    @Test
    void refreshUsesCurrentRoleRatherThanTokenClaimRole() throws Exception {
        Claims claims = mock(Claims.class);
        given(tokenProvider.parse("old-refresh-token")).willReturn(claims);
        given(tokenProvider.isRefreshToken(claims)).willReturn(true);
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(1L, Role.ADMIN);
        given(tokenProvider.toAuthenticatedMember(claims)).willReturn(authenticatedMember);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        given(memberService.findById(1L)).willReturn(new Member(1L, "user@example.com", "encoded", Role.MEMBER, now));
        given(tokenProvider.generateAccessToken(1L, Role.MEMBER)).willReturn("new-access-token");
        given(tokenProvider.generateRefreshToken(1L, Role.MEMBER)).willReturn("new-refresh-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"old-refresh-token\"}"))
            .andExpect(status().isOk());

        verify(tokenProvider).generateAccessToken(1L, Role.MEMBER);
        verify(tokenProvider, never()).generateAccessToken(1L, Role.ADMIN);
    }

    @Test
    void rejectsMalformedRefreshToken() throws Exception {
        given(tokenProvider.parse(any())).willThrow(new MalformedJwtException("malformed"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"garbage\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andDo(document("auth-refresh-invalid"));
    }

    @Test
    void rejectsAccessTokenUsedAsRefreshToken() throws Exception {
        Claims claims = mock(Claims.class);
        given(tokenProvider.parse(any())).willReturn(claims);
        given(tokenProvider.isRefreshToken(claims)).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"an-access-token\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsRefreshTokenNotMatchingStoredValue() throws Exception {
        Claims claims = mock(Claims.class);
        given(tokenProvider.parse(any())).willReturn(claims);
        given(tokenProvider.isRefreshToken(claims)).willReturn(true);
        given(tokenProvider.toAuthenticatedMember(claims)).willReturn(new AuthenticatedMember(1L, Role.MEMBER));
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        given(memberService.findById(1L)).willReturn(new Member(1L, "user@example.com", "encoded", Role.MEMBER, now));
        willThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).given(memberService)
            .rotateRefreshToken(eq(1L), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"stale-refresh-token\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsRefreshForUnknownMember() throws Exception {
        Claims claims = mock(Claims.class);
        given(tokenProvider.parse(any())).willReturn(claims);
        given(tokenProvider.isRefreshToken(claims)).willReturn(true);
        given(tokenProvider.toAuthenticatedMember(claims)).willReturn(new AuthenticatedMember(1L, Role.MEMBER));
        willThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).given(memberService).findById(1L);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"stale-refresh-token\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void logsOutAuthenticatedMember() throws Exception {
        Claims claims = mock(Claims.class);
        given(tokenProvider.parse("access-token")).willReturn(claims);
        given(tokenProvider.isAccessToken(claims)).willReturn(true);
        given(tokenProvider.toAuthenticatedMember(claims)).willReturn(new AuthenticatedMember(1L, Role.MEMBER));

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("auth-logout"));

        verify(memberService).revokeRefreshToken(1L);
    }

    @Test
    void rejectsLogoutWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

}
