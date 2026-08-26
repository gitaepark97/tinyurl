package com.hugo.tinyurl.member.web;

import com.hugo.tinyurl.common.web.response.ApiResponse;
import com.hugo.tinyurl.member.application.MemberService;
import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.web.request.MemberRoleUpdateRequest;
import com.hugo.tinyurl.member.web.response.MemberRoleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AdminMemberController {

    private final MemberService memberService;

    @PatchMapping("/api/v1/admin/members/{id}/role")
    ApiResponse<MemberRoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody MemberRoleUpdateRequest request) {
        Member member = memberService.updateRole(id, request.role());
        // 이미 발급된 refresh token으로 예전 role이 담긴 토큰을 재발급받지 못하도록 즉시 무효화한다.
        memberService.revokeRefreshToken(id);
        return ApiResponse.success(MemberRoleResponse.from(member));
    }

}
