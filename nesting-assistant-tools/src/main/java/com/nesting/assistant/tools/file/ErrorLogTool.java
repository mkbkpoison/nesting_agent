package com.nesting.assistant.tools.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 错误日志获取工具
 */
@Slf4j
@Service
public class ErrorLogTool {

    @Tool(description = "获取最近的错误日志记录，快速定位系统问题。" +
            "返回错误时间、错误代码、错误消息和堆栈摘要。" +
            "当需要快速查看系统错误时使用此工具。")
    public List<Map<String, Object>> getRecentErrorLogs(
            @ToolParam(description = "返回的最大记录数，默认10", required = false) Integer limit
    ) {
        int maxResults = limit != null ? limit : 10;
        log.info("Getting recent error logs, limit={}", maxResults);

        List<Map<String, Object>> errors = new ArrayList<>();

        // 模拟错误日志数据
        errors.add(createErrorEntry(
                "2026-05-18 10:30:45",
                "NEST-001",
                "ERROR",
                "NestingEngine",
                "内存不足，套料计算中断",
                "java.lang.OutOfMemoryError: Java heap space"
        ));
        errors.add(createErrorEntry(
                "2026-05-18 09:15:22",
                "NEST-003",
                "ERROR",
                "Database",
                "数据库连接超时",
                "java.sql.SQLException: Connection timed out"
        ));
        errors.add(createErrorEntry(
                "2026-05-18 08:45:10",
                "NEST-006",
                "ERROR",
                "Cutting",
                "切割路径生成失败",
                "PathGenerationException: Invalid contour"
        ));

        return errors.stream().limit(maxResults).toList();
    }

    private Map<String, Object> createErrorEntry(String timestamp, String errorCode,
                                                  String level, String module,
                                                  String message, String stackTrace) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", timestamp);
        entry.put("errorCode", errorCode);
        entry.put("level", level);
        entry.put("module", module);
        entry.put("message", message);
        entry.put("stackTracePreview", stackTrace);
        entry.put("occurrenceCount", 1);
        entry.put("firstOccurrence", timestamp);
        entry.put("lastOccurrence", timestamp);
        return entry;
    }
}
