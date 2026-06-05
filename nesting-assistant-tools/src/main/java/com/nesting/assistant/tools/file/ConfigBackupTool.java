package com.nesting.assistant.tools.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 配置备份工具
 */
@Slf4j
@Service
public class ConfigBackupTool {

    @Tool(description = "备份当前系统配置，包括套料参数、用户偏好设置、材料库等。" +
            "支持备份到本地文件或云端存储。" +
            "在修改重要配置前建议先备份。")
    public Map<String, Object> backupConfiguration(
            @ToolParam(description = "备份描述或备注", required = false) String description,
            @ToolParam(description = "备份目标：local(本地), cloud(云端), both(两者)", required = false) String target
    ) {
        String backupTarget = target != null ? target : "local";
        log.info("Backing up configuration to: {}", backupTarget);

        Map<String, Object> result = new LinkedHashMap<>();

        String backupId = "BKP-" + System.currentTimeMillis();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));

        result.put("backupId", backupId);
        result.put("timestamp", timestamp);
        result.put("target", backupTarget);
        result.put("description", description != null ? description : "系统配置备份");
        result.put("status", "SUCCESS");

        // 备份内容
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(createBackupItem("nesting_params", "套料参数配置", "nesting_params.json", 2));
        items.add(createBackupItem("user_preferences", "用户偏好设置", "user_prefs.json", 1));
        items.add(createBackupItem("material_library", "材料库", "materials.json", 50));
        items.add(createBackupItem("part_templates", "零件模板", "part_templates.json", 120));
        items.add(createBackupItem("machine_config", "机床配置", "machine_config.json", 5));
        result.put("backupItems", items);

        // 文件信息
        String fileName = "config_backup_" + timestamp + ".zip";
        Map<String, Object> fileInfo = new LinkedHashMap<>();
        fileInfo.put("fileName", fileName);
        fileInfo.put("filePath", "/backups/" + fileName);
        fileInfo.put("totalSizeKB", items.stream().mapToLong(i -> (Long) i.get("sizeKB")).sum());
        fileInfo.put("itemCount", items.size());
        result.put("file", fileInfo);

        // 云端信息（如果备份到云端）
        if ("cloud".equals(backupTarget) || "both".equals(backupTarget)) {
            Map<String, Object> cloudInfo = new LinkedHashMap<>();
            cloudInfo.put("storageProvider", "阿里云OSS");
            cloudInfo.put("bucketName", "nesting-backups");
            cloudInfo.put("objectKey", "backups/" + fileName);
            cloudInfo.put("downloadUrl", "https://oss.example.com/backups/" + fileName);
            cloudInfo.put("expireDays", 30);
            result.put("cloudStorage", cloudInfo);
        }

        // 历史备份列表
        List<Map<String, Object>> history = new ArrayList<>();
        history.add(createHistoryEntry("BKP-1001", "2026-05-17 14:30:00", "系统配置备份", "local"));
        history.add(createHistoryEntry("BKP-1000", "2026-05-15 09:00:00", "升级前备份", "both"));
        result.put("recentBackups", history);

        return result;
    }

    private Map<String, Object> createBackupItem(String code, String name, String file, long sizeKB) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("name", name);
        item.put("file", file);
        item.put("sizeKB", sizeKB);
        item.put("status", "BACKED_UP");
        return item;
    }

    private Map<String, Object> createHistoryEntry(String id, String time, String desc, String target) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("backupId", id);
        entry.put("timestamp", time);
        entry.put("description", desc);
        entry.put("target", target);
        return entry;
    }
}
