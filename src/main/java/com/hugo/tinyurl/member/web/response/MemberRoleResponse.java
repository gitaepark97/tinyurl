package com.hugo.tinyurl.member.web.response;

import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.model.Role;

public record MemberRoleResponse(
    Long id,
    String email,
    Role role
) {

    public static MemberRoleResponse from(Member member) {
        return new MemberRoleResponse(member.id(), member.email(), member.role());
    }

}
