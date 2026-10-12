package com.lanlink.shopping.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimitInterceptorTest {

    private AuthRateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthRateLimitInterceptor();
    }

    @Test
    void limitsRepeatedLoginAttemptsPerClient() throws Exception {
        for (int i = 0; i < 8; i++) {
            assertTrue(call("/auth/login", "198.51.100.23", null, null).allowed());
        }

        Result blocked = call("/auth/login", "198.51.100.23", null, null);
        assertFalse(blocked.allowed());
        org.junit.jupiter.api.Assertions.assertEquals(429, blocked.response().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("60", blocked.response().getHeader("Retry-After"));
    }

    @Test
    void registrationHasItsOwnLowerLimit() throws Exception {
        for (int i = 0; i < 4; i++) {
            assertTrue(call("/auth/register", "198.51.100.24", null, null).allowed());
        }
        assertFalse(call("/auth/register", "198.51.100.24", null, null).allowed());

        // Login policy is independent from the registration policy.
        assertTrue(call("/auth/login", "198.51.100.24", null, null).allowed());
    }

    @Test
    void ignoresOtherRoutesAndNonPostRequests() throws Exception {
        assertTrue(call("GET", "/auth/login", "198.51.100.25", null, null).allowed());
        assertTrue(call("/auth/me", "198.51.100.25", null, null).allowed());
    }

    @Test
    void forgedCloudflareHeaderCannotBypassIngressCeiling() throws Exception {
        // A direct caller can forge CF-Connecting-IP, but Railway's X-Real-IP
        // ingress bucket still caps requests from that actual network peer.
        for (int i = 1; i <= 120; i++) {
            assertTrue(call("/auth/login", "198.51.100.26", "203.0.113." + i, null).allowed());
        }
        assertFalse(call("/auth/login", "198.51.100.26", "203.0.113.200", null).allowed());
    }

    private Result call(String path, String xRealIp, String cloudflareIp, String vercelId) throws Exception {
        return call("POST", path, xRealIp, cloudflareIp, vercelId);
    }

    private Result call(String method, String path, String xRealIp, String cloudflareIp, String vercelId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setContextPath("/api");
        request.setRequestURI("/api" + path);
        if (xRealIp != null) request.addHeader("X-Real-IP", xRealIp);
        if (cloudflareIp != null) request.addHeader("CF-Connecting-IP", cloudflareIp);
        if (vercelId != null) request.addHeader("x-vercel-id", vercelId);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(request, response, new Object());
        return new Result(allowed, response);
    }

    private record Result(boolean allowed, MockHttpServletResponse response) {
    }
}
