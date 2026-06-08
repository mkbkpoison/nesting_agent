package com.nesting.assistant.api.config;

import com.nesting.assistant.api.auth.TokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 * 注册Token拦截器，保护聊天API
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;
    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流拦截器（优先）
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/v1/chat/**");

        // Token 认证拦截器
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/v1/chat/**")      // 保护聊天接口
                .addPathPatterns("/api/v1/knowledge/**")  // 保护知识库接口
                .addPathPatterns("/api/v1/diagnosis/**")  // 保护诊断接口
                .excludePathPatterns("/api/v1/auth/**")   // 放行认证接口
                .excludePathPatterns("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**") // 放行文档
                .excludePathPatterns("/actuator/**");     // 放行健康检查
    }
}
