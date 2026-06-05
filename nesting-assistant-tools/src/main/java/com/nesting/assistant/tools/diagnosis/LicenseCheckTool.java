package com.nesting.assistant.tools.diagnosis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 许可证状态检查工具
 */
@Slf4j
@Service
public class LicenseCheckTool {

    @Tool(description = "检查软件许可证状态，包括有效期、授权功能模块等信息。" +
            "当用户遇到许可证相关问题或需要确认授权状态时使用此工具。")
    public Map<String, Object> checkLicenseStatus() {
        log.info("Checking license status");

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 模拟许可证信息
            result.put("status", "VALID");
            result.put("licenseType", "Enterprise");
            result.put("licenseId", "NEST-ENT-2026-001");
            result.put("licensedTo", "示例制造有限公司");

            // 有效期信息
            LocalDate expiryDate = LocalDate.now().plusDays(30);
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

            Map<String, Object> validity = new LinkedHashMap<>();
            validity.put("startDate", "2025-01-01");
            validity.put("expiryDate", expiryDate.toString());
            validity.put("daysRemaining", daysRemaining);
            validity.put("isExpiringSoon", daysRemaining <= 30);
            result.put("validity", validity);

            // 授权功能模块
            List<Map<String, Object>> modules = new ArrayList<>();
            modules.add(createModuleEntry("NestingCore", "核心套料引擎", true));
            modules.add(createModuleEntry("AdvancedOptimization", "高级优化算法", true));
            modules.add(createModuleEntry("CncIntegration", "CNC集成", true));
            modules.add(createModuleEntry("ReportGeneration", "报表生成", true));
            modules.add(createModuleEntry("CloudSync", "云端同步", false));
            result.put("authorizedModules", modules);

            // 机器绑定信息
            Map<String, Object> machineBinding = new LinkedHashMap<>();
            machineBinding.put("machineId", "MCH-2024-001234");
            machineBinding.put("lastVerified", LocalDate.now().toString());
            result.put("machineBinding", machineBinding);

            // 警告信息
            List<String> warnings = new ArrayList<>();
            if (daysRemaining <= 30) {
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

    private Map<String, Object> createModuleEntry(String code, String name, boolean enabled) {
        Map<String, Object> module = new LinkedHashMap<>();
        module.put("code", code);
        module.put("name", name);
        module.put("enabled", enabled);
        return module;
    }
}
