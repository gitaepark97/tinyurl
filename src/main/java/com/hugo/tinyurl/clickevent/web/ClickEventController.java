package com.hugo.tinyurl.clickevent.web;

import com.hugo.tinyurl.clickevent.application.ClickEventService;
import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.clickevent.web.response.ClickEventResponse;
import com.hugo.tinyurl.domain.model.Role;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import com.hugo.tinyurl.support.response.ApiResponse;
import com.hugo.tinyurl.web.security.AuthenticatedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ClickEventController {

    private final ClickEventService clickEventService;

    @GetMapping("/api/v1/urls/{id}/click-events")
    ApiResponse<Page<ClickEventResponse>> findAll(
        @PathVariable Long id,
        @ModelAttribute PageParam pageParam,
        Authentication authentication
    ) {
        Long requesterMemberId = AuthenticatedMember.memberIdOrNull(authentication);
        Role requesterRole = AuthenticatedMember.roleOrNull(authentication);
        Page<ClickEvent> page = clickEventService.findAll(id, requesterMemberId, requesterRole, pageParam);
        return ApiResponse.success(page.map(ClickEventResponse::from));
    }

}
