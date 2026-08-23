package com.hugo.tinyurl.common.web.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    // X-Forwarded-For는 위조 가능해 직접 파싱하지 않고, server.forward-headers-strategy(RemoteIpValve)에 위임한다.
    public static String resolve(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

}
