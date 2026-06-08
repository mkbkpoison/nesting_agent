package com.nesting.assistant.tools.file;

import com.nesting.assistant.domain.entity.DiagnosticsLog;
import com.nesting.assistant.domain.repository.DiagnosticsLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogTool {

    private final DiagnosticsLogMapper diagnosticsLogMapper;

    @Tool(description = "获取最近的错误日志记录，快速定位系统问题。" +
            "返回错误时间、错误代码、错误消息和堆栈摘要。" +
            "当需要快速查看系统错误时使用此工具。")
    public List<Map<String, Object>> getRecentErrorLogs(
            @ToolParam(description = "返回的最大记录数，默认10", required = false) Integer limit
    ) {
        int maxResults = limit != null ? limit : 10;
        log.info("Getting recent error logs, limit={}", maxResults);

        // 从 DB 读取最近的诊断日志
        List<DiagnosticsLog> recentLogs = diagnosticsLogMapper
                .findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime.now().minusDays(7));

        return recentLogs.stream()
                .filter(dl -> dl.getStatus() != null &&
                        (dl.getStatus().equalsIgnoreCase("ERROR") || dl.getStatus().equalsIgnoreCase("FAILED")))
                .limit(maxResults)
                .map(dl -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("timestamp", dl.getCreatedAt() != null
                            ? dl.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            : "N/A");
                    entry.put("errorCode", dl.getDiagnosisType() != null ? dl.getDiagnosisType() : "N/A");
                    entry.put("level", dl.getStatus() != null ? dl.getStatus().toUpperCase() : "ERROR");
                    entry.put("module", dl.getDiagnosisType() != null ? dl.getDiagnosisType() : "System");
                    entry.put("message", dl.getSummary() != null ? dl.getSummary() : dl.getDetails());
                    entry.put("stackTracePreview", dl.getDetails() != null
                            ? (dl.getDetails().length() > 200 ? dl.getDetails().substring(0, 200) + "..." : dl.getDetails())
                            : "");
                    entry.put("occurrenceCount", 1);
                    entry.put("firstOccurrence", entry.get("timestamp"));
                    entry.put("lastOccurrence", entry.get("timestamp"));
                    return entry;
                })
                .collect(Collectors.toList());
    }
}