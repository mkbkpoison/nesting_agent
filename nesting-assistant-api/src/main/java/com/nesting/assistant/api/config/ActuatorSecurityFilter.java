package com.nesting.assistant.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 保护 Actuator 端点。
 * 所有 /actuator/** 请求必须携带 X-Internal-Key 头且值匹配配置。
 */
@Slf4j
@Component
@Order(1)
public class ActuatorSecurityFilter implements Filter {

    @Value("${nesting.assistant.internal-key:}")
    private String internalKey;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        if (path.startsWith("/actuator/")) {
            // 如果配置了 internal-key 则校验，否则放行（开发模式）
            if (!internalKey.isEmpty()) {
                String providedKey = request.getHeader("X-Internal-Key");
                if (!internalKey.equals(providedKey)) {
                    log.warn("Blocked actuator access from {}: {}", request.getRemoteAddr(), path);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                    return;
                }
            }
        }
        chain.doFilter(servletRequest, servletResponse);
    }
}