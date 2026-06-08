package com.nesting.assistant.tools.diagnosis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseCheckTool {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_LICENSE = "nesting:license";
    private static final String KEY_LICENSE_EXPIRY = "nesting:license:expiry";
    private static final String KEY_LICENSE_MODULES = "nesting:license:modules";

    @Tool(description = "检查软件许可证状态，包括有效期、授权功能模块等信息。" +
            "当用户遇到许可证相关问题或需要确认授权状态时使用此工具。")
    public Map<String, Object> checkLicenseStatus() {
        log.info("Checking license status");

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 从 Redis 读取许可证信息，如果不存在则使用默认值（开发模式）
            String licenseId = redisTemplate.opsForValue().get(KEY_LICENSE);
            String expiryStr = redisTemplate.opsForValue().get(KEY_LICENSE_EXPIRY);

            if (licenseId == null) {
                // 开发模式：初始化默认许可证
                licenseId = "NEST-DEV-2026-001";
                expiryStr = LocalDate.now().plusDays(90).toString();
                redisTemplate.opsForValue().set(KEY_LICENSE, licenseId, 1, TimeUnit.DAYS);
                redisTemplate.opsForValue().set(KEY_LICENSE_EXPIRY, expiryStr, 1, TimeUnit.DAYS);
                log.info("Initialized dev license in Redis: {}, expiry={}", licenseId, expiryStr);
            }

            LocalDate expiryDate = LocalDate.parse(expiryStr);
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

            result.put("status", daysRemaining > 0 ? "VALID" : "EXPIRED");
            result.put("licenseType", daysRemaining > 30 ? "Enterprise" : "Trial");
            result.put("licenseId", licenseId);
            result.put("licensedTo", "套料软件用户");

            Map<String, Object> validity = new LinkedHashMap<>();
            validity.put("startDate", LocalDate.now().minusDays(365).toString());
            validity.put("expiryDate", expiryStr);
            validity.put("daysRemaining", Math.max(0, daysRemaining));
            validity.put("isExpiringSoon", daysRemaining > 0 && daysRemaining <= 30);
            result.put("validity", validity);

            // 授权模块
            String modulesStr = redisTemplate.opsForValue().get(KEY_LICENSE_MODULES);
            List<Map<String, Object>> modules = new ArrayList<>();
            if (modulesStr != null) {
                for (String m : modulesStr.split(",")) {
                    Map<String, Object> module = new LinkedHashMap<>();
                    module.put("code", m.trim());
                    module.put("name", m.trim());
                    module.put("enabled", true);
                    modules.add(module);
                }
            }
            if (modules.isEmpty()) {
                // 默认模块
                for (String m : new String[]{"NestingCore", "AdvancedOptimization", "ReportGeneration"}) {
                    Map<String, Object> module = new LinkedHashMap<>();
                    module.put("code", m);
                    module.put("name", m);
                    module.put("enabled", true);
                    modules.add(module);
                }
            }
            result.put("authorizedModules", modules);

            // 警告
            List<String> warnings = new ArrayList<>();
            if (daysRemaining <= 0) {
                warnings.add("许可证已过期，请立即续订");
            } else if (daysRemaining <= 30) {
                warnings.add("许可证将在" + daysRemaining + "天后过期，请联系供应商续订");
            }
            result.put("warnings", warnings);

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            log.error("License check failed", e);
        }

        return result;
    }
}