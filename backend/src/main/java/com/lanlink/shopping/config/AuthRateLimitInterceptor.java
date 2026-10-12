package com.lanlink.shopping.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanlink.shopping.common.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Endpoint-specific throttling for public authentication routes.
 *
 * Railway's public edge overwrites X-Real-IP. CF-Connecting-IP is used only as
 * an additional per-client dimension because direct access to the Railway
 * origin can supply that header; every request is also charged to X-Real-IP.
 */
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final long IDLE_TTL_MILLIS = 15 * 60_000L;
    private static final int MAX_COUNTERS = 20_000;

    private static final Map<String, Policy> POLICIES = Map.of(
            "/auth/login", new Policy(8, 120),
            "/auth/login/2fa", new Policy(8, 120),
            "/auth/register", new Policy(4, 30)
    );

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong handledRequests = new AtomicLong();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = applicationPath(request);
        Policy policy = POLICIES.get(path);
        if (policy == null) {
            return true;
        }

        long now = System.currentTimeMillis();
        String railwayIp = normalizeIp(request.getHeader("X-Real-IP"));
        if (railwayIp == null) {
            railwayIp = normalizeIp(request.getRemoteAddr());
        }
        if (railwayIp == null) {
            railwayIp = "unknown";
        }

        // Always apply the Railway-ingress limit; this cannot be bypassed by
        // changing the optional CF-Connecting-IP header on direct-origin calls.
        boolean ingressAllowed = allow("ingress|" + path + "|" + railwayIp, policy.ingressLimit, now);

        String clientIp = normalizeIp(request.getHeader("CF-Connecting-IP"));
        if (clientIp == null && !isVercelForwardedRequest(request)) {
            // Direct requests to the Railway public endpoint have a sanitized
            // X-Real-IP. Avoid using the Vercel egress address as a shared
            // client key if Vercel did not preserve CF-Connecting-IP.
            clientIp = railwayIp.equals("unknown") ? null : railwayIp;
        }
        boolean clientAllowed = clientIp == null
                || allow("client|" + path + "|" + clientIp, policy.clientLimit, now);

        cleanupIfNeeded(now);
        if (ingressAllowed && clientAllowed) {
            return true;
        }

        response.setStatus(429);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", "60");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(objectMapper.writeValueAsString(
                R.fail(429, "认证请求过于频繁，请稍后再试")));
        return false;
    }

    private boolean allow(String key, int limit, long now) {
        WindowCounter counter = counters.get(key);
        if (counter == null) {
            if (counters.size() >= MAX_COUNTERS) {
                cleanupStaleCounters(now);
            }
            if (counters.size() >= MAX_COUNTERS) {
                return false; // fail closed rather than grow without bound
            }
            WindowCounter candidate = new WindowCounter(now);
            WindowCounter previous = counters.putIfAbsent(key, candidate);
            counter = previous == null ? candidate : previous;
        }
        return counter.tryAcquire(limit, now);
    }

    private void cleanupIfNeeded(long now) {
        if ((handledRequests.incrementAndGet() & 0xffL) == 0L || counters.size() >= MAX_COUNTERS) {
            cleanupStaleCounters(now);
        }
    }

    private void cleanupStaleCounters(long now) {
        counters.entrySet().removeIf(entry -> entry.getValue().isIdle(now, IDLE_TTL_MILLIS));
    }

    private static String applicationPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (uri == null || uri.isBlank()) {
            uri = request.getServletPath();
        }
        if (uri == null) {
            return "";
        }
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }

    private static boolean isVercelForwardedRequest(HttpServletRequest request) {
        return hasText(request.getHeader("x-vercel-id"))
                || hasText(request.getHeader("x-vercel-deployment-url"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Accept only IP literals. Never resolve arbitrary header values as DNS
     * names and never fall back to X-Forwarded-For's untrusted proxy chain.
     */
    private static String normalizeIp(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || value.length() > 64 || value.indexOf(',') >= 0
                || value.indexOf('%') >= 0 || value.startsWith("[") || value.endsWith("]")) {
            return null;
        }

        if (value.indexOf(':') >= 0) {
            if (!value.matches("[0-9a-fA-F:]+")) {
                return null;
            }
            try {
                return InetAddress.getByName(value).getHostAddress().toLowerCase(Locale.ROOT);
            } catch (Exception ignored) {
                return null;
            }
        }

        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return null;
        }
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3 || !octet.chars().allMatch(Character::isDigit)) {
                return null;
            }
            try {
                if (Integer.parseInt(octet) > 255) {
                    return null;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return value;
    }

    private record Policy(int clientLimit, int ingressLimit) {
    }

    private static final class WindowCounter {
        private long windowStart;
        private long lastSeen;
        private int count;

        private WindowCounter(long now) {
            this.windowStart = now;
            this.lastSeen = now;
        }

        private synchronized boolean tryAcquire(int limit, long now) {
            lastSeen = now;
            if (now < windowStart || now - windowStart >= WINDOW_MILLIS) {
                windowStart = now;
                count = 0;
            }
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }

        private synchronized boolean isIdle(long now, long idleTtlMillis) {
            return now >= lastSeen && now - lastSeen >= idleTtlMillis;
        }
    }
}
