package com.bikepooling.config;

import com.bikepooling.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed rate limiter.
 *
 * IP limits  — applied to public auth endpoints (no JWT needed)
 * User limits — applied to authenticated API endpoints
 *
 * Note: @Order is intentionally absent. Adding @Order to a @Component filter
 * registers it in the root servlet filter chain, causing it to run twice
 * (once outside Spring Security, once inside). Ordering is controlled solely
 * by SecurityConfig.addFilterBefore().
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil             jwtUtil;

    private static final Map<String, Integer> IP_LIMITS = Map.of(
            "/api/auth/register",                 5,
            "/api/auth/verify-registration-otp", 10,
            "/api/auth/login/send-otp",           5,
            "/api/auth/login/verify-otp",         10,
            "/api/auth/login/password",           10,
            "/api/auth/admin/login/send-otp",     5,
            "/api/auth/admin/login/verify-otp",   10
    );

    private static final Map<String, Integer> USER_LIMITS = Map.of(
            "/api/rides",    30,
            "/api/vehicles", 20
    );

    private static final Duration WINDOW_1_MIN = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // IP-based rate limit for public auth endpoints
        if (IP_LIMITS.containsKey(path)) {
            if (isRateLimited(
                    "rl:ip:" + getClientIp(request) + ":" + path,
                    IP_LIMITS.get(path),
                    WINDOW_1_MIN,
                    response)) return;

            chain.doFilter(request, response);
            return;
        }

        // User-based rate limit for authenticated endpoints
        String userId = extractUserId(request);
        if (userId == null) {
            chain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/rides") || path.startsWith("/api/vehicles")) {
            String prefix = path.startsWith("/api/rides") ? "rides" : "vehicles";
            if (isRateLimited(
                    "rl:user:" + userId + ":" + prefix,
                    USER_LIMITS.getOrDefault("/api/" + prefix, 60),
                    WINDOW_1_MIN,
                    response)) return;
        }

        chain.doFilter(request, response);
    }

    // Lua script: atomically increment and set TTL on first creation.
    // Prevents TOCTOU bug where crash between INCR and EXPIRE leaves key without TTL.
    private static final String RATE_LIMIT_LUA =
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return count";

    private boolean isRateLimited(String key, int limit,
                                  Duration window,
                                  HttpServletResponse response) throws IOException {
        Long count;
        try {
            count = redisTemplate.execute(
                    org.springframework.data.redis.core.script.RedisScript.of(RATE_LIMIT_LUA, Long.class),
                    java.util.List.of(key),
                    String.valueOf(window.getSeconds()));
        } catch (Exception e) {
            log.warn("Redis rate-limit check failed, allowing request: {}", e.getMessage());
            return false; // Fail-open: allow request if Redis is down
        }
        if (count != null && count > limit) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"data\":null," +
                            "\"message\":\"Too many requests. Please slow down.\"}");
            log.warn("Rate limit hit: key={} count={} limit={}", key, count, limit);
            return true;
        }
        return false;
    }

    private String extractUserId(HttpServletRequest request) {
        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) return null;
            return String.valueOf(jwtUtil.extractUserId(header.substring(7)));
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}