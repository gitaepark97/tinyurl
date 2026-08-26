package com.hugo.tinyurl.member.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

// 인증/권한(hasRole)으로 분기하는 로직을 검증해야 해서 실제 SecurityConfig를 끌어와 필터 체인을 적용한다.
@WebMvcTest(controllers = AdminMemberController.class)
@Import({SecurityConfig.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
@AutoConfigureRestDocs
class AdminMemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    TokenProvider tokenProvider;

    @Test
    void updatesMemberRoleAsAdmin() throws Exception {
        Member member = newMember(10L, "user@example.com", Role.ADMIN);
        given(memberService.updateRole(eq(10L), eq(Role.ADMIN))).willReturn(member);
        stubAuthenticatedMember("admin-access-token", new AuthenticatedMember(99L, Role.ADMIN));

        mockMvc.perform(patch("/api/v1/admin/members/{id}/role", 10L)
                .header("Authorization", "Bearer admin-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(10))
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
            .andDo(document("admin-member-role-update",
                pathParameters(parameterWithName("id").description("회원 id")),
                requestFields(fieldWithPath("role").description("변경할 role(MEMBER 또는 ADMIN)")),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.id").description("회원 id"),
                    fieldWithPath("data.email").description("이메일"),
                    fieldWithPath("data.role").description("변경된 role"),
                    fieldWithPath("message").description("에러 메시지(성공 시 null)")
                )));

        verify(memberService).revokeRefreshToken(10L);
    }

    @Test
    void rejectsRoleUpdateFromAnonymous() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/members/{id}/role", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsRoleUpdateFromNonAdminMember() throws Exception {
        stubAuthenticatedMember("member-access-token", new AuthenticatedMember(10L, Role.MEMBER));

        mockMvc.perform(patch("/api/v1/admin/members/{id}/role", 10L)
                .header("Authorization", "Bearer member-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void returnsNotFoundForUnknownMember() throws Exception {
        given(memberService.updateRole(eq(999L), eq(Role.ADMIN))).willThrow(new BusinessException(ErrorCode.NOT_FOUND));
        stubAuthenticatedMember("admin-access-token", new AuthenticatedMember(99L, Role.ADMIN));

        mockMvc.perform(patch("/api/v1/admin/members/{id}/role", 999L)
                .header("Authorization", "Bearer admin-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andDo(document("admin-member-role-update-not-found"));
    }

    @Test
    void rejectsDemotingLastAdmin() throws Exception {
        given(memberService.updateRole(eq(10L), eq(Role.MEMBER))).willThrow(new BusinessException(ErrorCode.LAST_ADMIN_DEMOTION));
        stubAuthenticatedMember("admin-access-token", new AuthenticatedMember(10L, Role.ADMIN));

        mockMvc.perform(patch("/api/v1/admin/members/{id}/role", 10L)
                .header("Authorization", "Bearer admin-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"MEMBER\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("LAST_ADMIN_DEMOTION"))
            .andDo(document("admin-member-role-update-last-admin-conflict"));
    }

    @Test
    void rejectsInvalidRoleValue() throws Exception {
        stubAuthenticatedMember("admin-access-token", new AuthenticatedMember(99L, Role.ADMIN));

        mockMvc.perform(patch("/api/v1/admin/members/{id}/role", 10L)
                .header("Authorization", "Bearer admin-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"SUPERADMIN\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void rejectsMissingRole() throws Exception {
        stubAuthenticatedMember("admin-access-token", new AuthenticatedMember(99L, Role.ADMIN));

        mockMvc.perform(patch("/api/v1/admin/members/{id}/role", 10L)
                .header("Authorization", "Bearer admin-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private void stubAuthenticatedMember(String token, AuthenticatedMember authenticatedMember) {
        Claims claims = mock(Claims.class);
        given(tokenProvider.parse(token)).willReturn(claims);
        given(tokenProvider.isAccessToken(claims)).willReturn(true);
        given(tokenProvider.toAuthenticatedMember(claims)).willReturn(authenticatedMember);
    }

    private Member newMember(Long id, String email, Role role) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return new Member(id, email, "encoded-password", role, now);
    }

}
