package com.hugo.tinyurl.web.controller.v1.response;

import com.hugo.tinyurl.domain.model.Member;
import com.hugo.tinyurl.domain.model.Role;

public record MemberRoleResponse(
    Long id,
    String email,
    Role role
) {

    public static MemberRoleResponse from(Member member) {
        return new MemberRoleResponse(member.id(), member.email(), member.role());
    }

}
