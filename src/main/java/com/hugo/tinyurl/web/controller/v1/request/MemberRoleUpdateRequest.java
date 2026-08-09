package com.hugo.tinyurl.web.controller.v1.request;

import com.hugo.tinyurl.domain.model.Role;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(
    @NotNull
    Role role
) {
}
