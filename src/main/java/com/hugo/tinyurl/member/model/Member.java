package com.hugo.tinyurl.member.model;

import com.hugo.tinyurl.common.model.Role;
import java.time.LocalDateTime;

public record Member(
    Long id,
    String email,
    String password,
    Role role,
    LocalDateTime createdAt
) {

    public static Member create(Long id, String email, String encodedPassword, LocalDateTime now) {
        return new Member(id, email, encodedPassword, Role.MEMBER, now);
    }

}
