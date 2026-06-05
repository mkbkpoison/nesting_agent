package com.nesting.assistant.api.auth;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Token拦截器
 * 从 Authorization header 提取 Token，验证后将 userId 注入 request attribute
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final String USER_ID_ATTR = "currentUserId";

    @Resource
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 请求（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header: {}", request.getRequestURI());
            response.setStatus(401);
            return false;
        }

        String token = authHeader.substring(7);
        Optional<String> userIdOpt = authService.validateToken(token);

        if (userIdOpt.isEmpty()) {
            log.warn("Invalid or expired token: {}", request.getRequestURI());
            response.setStatus(401);
            return false;
        }

        request.setAttribute(USER_ID_ATTR, userIdOpt.get());
        return true;
    }

    /**
     * 从 request 中获取当前用户ID
     */
    public static String getCurrentUserId(HttpServletRequest request) {
        Object uid = request.getAttribute(USER_ID_ATTR);
        return uid != null ? uid.toString() : "default";
    }
}
