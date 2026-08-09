package com.hugo.tinyurl.web.controller.v1.response;

import com.hugo.tinyurl.domain.model.Member;
import java.time.LocalDateTime;

public record MemberResponse(
    Long id,
    String email,
    LocalDateTime createdAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.id(), member.email(), member.createdAt());
    }

}
