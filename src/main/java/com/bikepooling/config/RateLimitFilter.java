package com.bikepooling.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    // endpoints to protect → max hits allowed per window
    private static final Map<String, Integer> LIMITS = Map.of(
            "/api/auth/register",                5,
            "/api/auth/verify-registration-otp", 10,
            "/api/auth/login/send-otp",          5,
            "/api/auth/login/verify-otp",        10,
            "/api/auth/login/password",          10
    );

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // skip if not a protected endpoint
        if (!LIMITS.containsKey(path)) {
            chain.doFilter(request, response);
            return;
        }

        String ip    = getClientIp(request);
        String key   = "rl:" + ip + ":" + path;
        int    limit = LIMITS.get(path);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, WINDOW);
        }

        if (count != null && count > limit) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"data\":null," +
                            "\"message\":\"Too many requests. Please slow down.\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}