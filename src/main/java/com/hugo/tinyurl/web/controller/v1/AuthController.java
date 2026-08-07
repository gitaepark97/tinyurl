package com.hugo.tinyurl.web.controller.v1;

import com.hugo.tinyurl.domain.application.MemberService;
import com.hugo.tinyurl.domain.model.Member;
import com.hugo.tinyurl.support.response.ApiResponse;
import com.hugo.tinyurl.web.controller.v1.request.SignupRequest;
import com.hugo.tinyurl.web.controller.v1.response.MemberResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AuthController {

    private final MemberService memberService;

    @PostMapping("/api/v1/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        Member member = memberService.register(request.email(), request.password());
        return ApiResponse.success(MemberResponse.from(member));
    }

}
