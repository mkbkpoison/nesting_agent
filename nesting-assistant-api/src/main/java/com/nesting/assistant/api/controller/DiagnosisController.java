package com.nesting.assistant.api.controller;

import com.nesting.assistant.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 诊断接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/diagnosis")
@RequiredArgsConstructor
@Tag(name = "诊断接口", description = "系统诊断API")
public class DiagnosisController {

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查系统健康状态")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", new Date());

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", Map.of("status", "UP"));
        components.put("redis", Map.of("status", "UP"));
        components.put("vectorStore", Map.of("status", "UP"));
        health.put("components", components);

        return ApiResponse.success(health);
    }

    @GetMapping("/system")
    @Operation(summary = "系统信息", description = "获取系统运行信息")
    public ApiResponse<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // JVM信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("maxMemory", runtime.maxMemory() / (1024 * 1024) + " MB");
        jvm.put("totalMemory", runtime.totalMemory() / (1024 * 1024) + " MB");
        jvm.put("freeMemory", runtime.freeMemory() / (1024 * 1024) + " MB");
        jvm.put("availableProcessors", runtime.availableProcessors());
        jvm.put("javaVersion", System.getProperty("java.version"));
        info.put("jvm", jvm);

        // 系统信息
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        system.put("osArch", System.getProperty("os.arch"));
        info.put("system", system);

        // 应用信息
        Map<String, Object> app = new LinkedHashMap<>();
        app.put("name", "Nesting Assistant");
        app.put("version", "1.0.0-SNAPSHOT");
        info.put("application", app);

        return ApiResponse.success(info);
    }
}
