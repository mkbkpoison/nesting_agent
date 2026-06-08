package com.nesting.assistant.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Redis 滑动窗口限流。
 * 按 userId 对 /api/v1/chat/** 限流，默认每分钟 30 次。
 */
@Slf4j
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final int windowSeconds;

    private static final String KEY_PREFIX = "nesting:ratelimit:";

    public RateLimitingInterceptor(StringRedisTemplate redisTemplate,
                                   @Value("${nesting.assistant.rate-limit.max:30}") int maxRequests,
                                   @Value("${nesting.assistant.rate-limit.window-seconds:60}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 只对聊天接口限流
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/chat/") && !path.startsWith("/api/v1/chat")) {
            return true;
        }

        String userId = (String) request.getAttribute("currentUserId");
        if (userId == null) {
            userId = "anonymous";
        }

        String key = KEY_PREFIX + userId + ":" + (System.currentTimeMillis() / (windowSeconds * 1000L));
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        if (count != null && count > maxRequests) {
            log.warn("Rate limit exceeded for userId={}, count={}", userId, count);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }

        return true;
    }
}