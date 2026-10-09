package com.lanlink.shopping.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanlink.shopping.common.R;
import com.lanlink.shopping.entity.User;
import com.lanlink.shopping.service.SysConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitInterceptor implements HandlerInterceptor {
    private static final int MAX_CLIENT_BUCKETS = 4096;
    private static final long IDLE_TTL_NANOS = TimeUnit.MINUTES.toNanos(10);
    private final SysConfigService config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, ClientBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();
    private final TokenBucket overflowBucket = new TokenBucket(200, 100);

    public RateLimitInterceptor(SysConfigService config) {
        this.config = config;
        int configured = config.getInt("ratelimit.qps", 100);
        int qps = configured <= 0 ? 100 : Math.min(configured, 10_000);
        overflowBucket.configure(qps * 2, qps);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Obje¶»§q«^