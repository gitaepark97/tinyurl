package com.hugo.tinyurl.shorturl.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.common.model.Role;
import com.hugo.tinyurl.member.web.security.TokenProvider;
import com.hugo.tinyurl.shorturl.web.request.ShortUrlCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
    "app.rate-limit.member-url-creation.capacity=3",
    "app.rate-limit.member-url-creation.refill-tokens=3",
    "app.rate-limit.member-url-creation.refill-duration-seconds=60"
})
class MemberShortUrlCreationRateLimitFilterTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TokenProvider tokenProvider;

    @Test
    void blocksMemberRequestsOnceCapacityIsExhausted() {
        HttpHeaders headers = authHeaders(1_000_001L);

        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/urls", HttpMethod.POST, createRequest(headers), String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        ResponseEntity<String> fourth = restTemplate.exchange(
            "/api/v1/urls", HttpMethod.POST, createRequest(headers), String.class);

        assertThat(fourth.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void countsDifferentMembersIndependently() {
        HttpHeaders exhaustedMember = authHeaders(1_000_002L);
        for (int i = 0; i < 3; i++) {
            restTemplate.exchange("/api/v1/urls", HttpMethod.POST, createRequest(exhaustedMember), String.class);
        }
        HttpHeaders otherMember = authHeaders(1_000_003L);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/urls", HttpMethod.POST, createRequest(otherMember), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void doesNotLimitAnonymousRequests() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "203.0.113.30");

        for (int i = 0; i < 4; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/urls", HttpMethod.POST, createRequest(headers), String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    private HttpHeaders authHeaders(Long memberId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.generateAccessToken(memberId, Role.MEMBER));
        return headers;
    }

    private HttpEntity<ShortUrlCreateRequest> createRequest(HttpHeaders headers) {
        ShortUrlCreateRequest body = new ShortUrlCreateRequest("https://example.com/" + System.nanoTime(), null, null);
        return new HttpEntity<>(body, headers);
    }

}
