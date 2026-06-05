package com.nesting.assistant.tools.diagnosis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 系统日志检查工具
 */
@Slf4j
@Service
public class SystemLogTool {

    @Tool(description = "检查系统日志，根据时间范围和错误级别筛选日志记录。" +
            "返回匹配的日志条目列表，包含时间戳、级别、模块和消息内容。" +
            "当用户需要排查系统问题或查看运行状态时使用此工具。")
    public List<Map<String, Object>> checkSystemLogs(
            @ToolParam(description = "时间范围：last_hour(最近1小时), last_day(最近24小时), last_week(最近7天)") String timeRange,
            @ToolParam(description = "错误级别过滤：ERROR, WARN, INFO, DEBUG，多个用逗号分隔，为空则返回所有级别", required = false) String errorLevel
    ) {
        log.info("Checking system logs: timeRange={}, errorLevel={}", timeRange, errorLevel);

        List<Map<String, Object>> logs = new ArrayList<>();

        // 模拟日志数据
        logs.add(createLogEntry("2026-05-18 10:30:45", "ERROR", "NestingEngine",
                "内存不足，套料计算中断", "nesting-worker-3"));
        logs.add(createLogEntry("2026-05-18 10:25:12", "WARN", "Database",
                "数据库连接池使用率达到80%", "db-pool-monitor"));
        logs.add(createLogEntry("2026-05-18 10:20:33", "INFO", "Import",
                "成功导入零件文件: panel_001.dxf", "import-service-1"));
        logs.add(createLogEntry("2026-05-18 10:15:00", "ERROR", "License",
                "许可证即将过期，剩余7天", "license-checker"));
        logs.add(createLogEntry("2026-05-18 10:10:22", "DEBUG", "NestingEngine",
                "开始套料计算，零件数量: 156", "nesting-worker-1"));

        // 根据错误级别过滤
        if (errorLevel != null && !errorLevel.isEmpty()) {
            Set<String> levels = new HashSet<>(Arrays.asList(errorLevel.split(",")));
            logs = logs.stream()
                    .filter(entry -> levels.contains(entry.get("level").toString()))
                    .toList();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", logs.size());
        result.put("timeRange", timeRange);
        result.put("filterLevel", errorLevel);
        result.put("logs", logs);

        return List.of(result);
    }

    private Map<String, Object> createLogEntry(String timestamp, String level,
                                                String module, String message, String thread) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", timestamp);
        entry.put("level", level);
        entry.put("module", module);
        entry.put("message", message);
        entry.put("thread", thread);
        return entry;
    }
}
