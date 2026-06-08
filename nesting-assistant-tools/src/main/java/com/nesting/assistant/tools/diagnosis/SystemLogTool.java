package com.nesting.assistant.tools.diagnosis;

import com.nesting.assistant.domain.entity.DiagnosticsLog;
import com.nesting.assistant.domain.repository.DiagnosticsLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogTool {

    private final DiagnosticsLogMapper diagnosticsLogMapper;

    @Tool(description = "检查系统日志，根据时间范围和错误级别筛选日志记录。" +
            "返回匹配的日志条目列表，包含时间戳、级别、模块和消息内容。" +
            "当用户需要排查系统问题或查看运行状态时使用此工具。")
    public List<Map<String, Object>> checkSystemLogs(
            @ToolParam(description = "时间范围：last_hour(最近1小时), last_day(最近24小时), last_week(最近7天)") String timeRange,
            @ToolParam(description = "错误级别过滤：ERROR, WARN, INFO, DEBUG，多个用逗号分隔，为空则返回所有级别", required = false) String errorLevel
    ) {
        log.info("Checking system logs: timeRange={}, errorLevel={}", timeRange, errorLevel);

        // 从 DB 读取诊断日志
        LocalDateTime since = switch (timeRange != null ? timeRange : "last_day") {
            case "last_hour" -> LocalDateTime.now().minusHours(1);
            case "last_week" -> LocalDateTime.now().minusDays(7);
            default -> LocalDateTime.now().minusDays(1);
        };
        List<DiagnosticsLog> dbLogs = diagnosticsLogMapper.findByCreatedAtAfterOrderByCreatedAtDesc(since);

        // 转为统一格式
        List<Map<String, Object>> logs = dbLogs.stream().map(dl -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("timestamp", dl.getCreatedAt() != null ? dl.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A");
            entry.put("level", dl.getStatus() != null ? dl.getStatus().toUpperCase() : "INFO");
            entry.put("module", dl.getDiagnosisType() != null ? dl.getDiagnosisType() : "System");
            entry.put("message", dl.getSummary() != null ? dl.getSummary() : dl.getDetails());
            entry.put("thread", "db-query");
            return entry;
        }).collect(Collectors.toList());

        // 也尝试从日志目录读取（如果配置了）
        String logDir = System.getProperty("nesting.log.dir", "");
        if (!logDir.isEmpty()) {
            try {
                File dir = new File(logDir);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles((f, name) -> name.endsWith(".log"));
                    if (files != null) {
                        for (File f : files) {
                            List<String> lines = Files.readAllLines(f.toPath());
                            for (String line : lines) {
                                Map<String, Object> entry = new LinkedHashMap<>();
                                entry.put("timestamp", "file");
                                entry.put("level", extractLevel(line));
                                entry.put("module", f.getName());
                                entry.put("message", line.length() > 200 ? line.substring(0, 200) : line);
                                entry.put("thread", "log-file");
                                logs.add(entry);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to read log directory: {}", e.getMessage());
            }
        }

        // 按级别过滤
        if (errorLevel != null && !errorLevel.isEmpty()) {
            Set<String> levels = new HashSet<>(Arrays.asList(errorLevel.toUpperCase().split(",")));
            logs = logs.stream()
                    .filter(entry -> entry.get("level") != null && levels.contains(entry.get("level").toString()))
                    .collect(Collectors.toList());
        }

        // 按时间排序（最新的在前）
        logs.sort((a, b) -> String.valueOf(b.get("timestamp")).compareTo(String.valueOf(a.get("timestamp"))));

        // 最多返回50条
        if (logs.size() > 50) logs = logs.subList(0, 50);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", logs.size());
        result.put("timeRange", timeRange);
        result.put("filterLevel", errorLevel);
        result.put("logs", logs);

        return List.of(result);
    }

    private String extractLevel(String line) {
        if (line.contains("ERROR")) return "ERROR";
        if (line.contains("WARN")) return "WARN";
        if (line.contains("DEBUG")) return "DEBUG";
        return "INFO";
    }
}