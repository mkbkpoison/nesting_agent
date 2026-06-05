package com.nesting.assistant.api.auth;

import com.nesting.assistant.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 注册、登录、登出
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "用户注册、登录、登出API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public ApiResponse<Map<String, Object>> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String nickname) {
        try {
            Map<String, Object> result = authService.register(username, password, nickname);
            return ApiResponse.success("注册成功", result);
        } catch (RuntimeException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录获取Token")
    public ApiResponse<Map<String, Object>> login(
            @RequestParam String username,
            @RequestParam String password) {
        try {
            Map<String, Object> result = authService.login(username, password);
            return ApiResponse.success("登录成功", result);
        } catch (RuntimeException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "登出并失效Token")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return ApiResponse.success("已登出", null);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "根据Token获取当前用户信息")
    public ApiResponse<Map<String, Object>> me(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        String token = authHeader.substring(7);
        var userIdOpt = authService.validateToken(token);
        if (userIdOpt.isEmpty()) {
            return ApiResponse.error(401, "Token无效或已过期");
        }
        var userOpt = authService.getUserById(userIdOpt.get());
        if (userOpt.isEmpty()) {
            return ApiResponse.error(401, "用户不存在");
        }
        var user = userOpt.get();
        return ApiResponse.success(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "nickname", user.getNickname()
        ));
    }
}
