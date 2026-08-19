package com.hugo.tinyurl.member.web.request;

import com.hugo.tinyurl.common.model.Role;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(
    @NotNull
    Role role
) {
}
