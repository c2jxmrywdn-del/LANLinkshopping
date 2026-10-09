package com.lanlink.shopping.config;

import com.lanlink.shopping.service.SysConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {
    @Test
    void bucketsAreIsolatedByClientIpAndReturn429AtLimit() throws Exception {
        SysConfigService config=mock(SysConfigService.class);
        when(config.getInt("ratelimit.qps",100)).thenReturn(1);
        RateLimitInterceptor interceptor=new RateLimitInterceptor(config);
        HttpServletResponse response=response();
        assertTrue(interceptor.preHandle(request("192.0.2.1"),response,new Object()));
        assertTrue(interceptor.preHandle(request("192.0.2.1"),response,new Object()));
        assertFalse(interceptor.preHandle(request("192.0.2.1"),response,new Object()));
        verify(response).setStatus(429);
        assertTrue(interceptor.preHandle(request("192.0.2.2"),response(),new Object()));
    }
    private HttpServletRequest request(String ip) {
        HttpServletRequest r=mock(HttpServletRequest.class);
        when(r.getRemoteAddr()).thenReturn(ip);
        when(r.getSession(false)).thenReturn(null);
        return r;
    }
    private HttpServletResponse response() throws Exception {
        HttpServletResponse r=mock(HttpServletResponse.class);
¶»§q«^