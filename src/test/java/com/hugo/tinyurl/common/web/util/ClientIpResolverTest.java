package com.hugo.tinyurl.common.web.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    // server.forward-headers-strategy(RemoteIpValve)에 위임하므로 여기서는 단순 위임만 검증한다.
    @Test
    void delegatesToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.1");
    }

}
