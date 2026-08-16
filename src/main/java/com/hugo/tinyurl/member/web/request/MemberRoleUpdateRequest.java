package com.hugo.tinyurl.member.web.request;

import com.hugo.tinyurl.member.model.Role;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(
    @NotNull
    Role role
) {
}
