package com.hugo.tinyurl.common.web.security;

import com.hugo.tinyurl.common.model.Role;
import org.springframework.security.core.Authentication;

public record AuthenticatedMember(Long memberId, Role role) {

    public static AuthenticatedMember from(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember) {
            return authenticatedMember;
        }
        return null;
    }

    public static Long memberIdOrNull(Authentication authentication) {
        AuthenticatedMember authenticatedMember = from(authentication);
        return authenticatedMember != null ? authenticatedMember.memberId() : null;
    }

    public static Role roleOrNull(Authentication authentication) {
        AuthenticatedMember authenticatedMember = from(authentication);
        return authenticatedMember != null ? authenticatedMember.role() : null;
    }

}
