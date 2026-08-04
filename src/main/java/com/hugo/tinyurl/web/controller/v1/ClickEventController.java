package com.hugo.tinyurl.web.controller.v1;

import com.hugo.tinyurl.domain.entity.ClickEvent;
import com.hugo.tinyurl.domain.service.ClickEventService;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import com.hugo.tinyurl.support.response.ApiResponse;
import com.hugo.tinyurl.web.controller.v1.response.ClickEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ClickEventController {

    private final ClickEventService clickEventService;

    @GetMapping("/api/v1/urls/{id}/click-events")
    ApiResponse<Page<ClickEventResponse>> findAll(@PathVariable Long id, @ModelAttribute PageParam pageParam) {
        Page<ClickEvent> page = clickEventService.findAll(id, pageParam);
        return ApiResponse.success(page.map(ClickEventResponse::from));
    }

}
