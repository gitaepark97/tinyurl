package com.hugo.tinyurl.web.security;

import com.hugo.tinyurl.domain.model.Role;

public record AuthenticatedMember(Long memberId, Role role) {
}
