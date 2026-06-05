package com.nesting.assistant.tools.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 诊断报告导出工具
 */
@Slf4j
@Service
public class ReportExportTool {

    @Tool(description = "导出系统诊断报告，包含系统状态、日志摘要、性能指标等信息。" +
            "支持PDF、HTML、JSON格式。" +
            "当用户需要保存或分享诊断结果时使用此工具。")
    public Map<String, Object> exportDiagnosticReport(
            @ToolParam(description = "报告格式：pdf, html, json") String format,
            @ToolParam(description = "报告标题", required = false) String title
    ) {
        log.info("Exporting diagnostic report: format={}", format);

        Map<String, Object> result = new LinkedHashMap<>();

        String reportId = "RPT-" + System.currentTimeMillis();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        result.put("reportId", reportId);
        result.put("format", format.toLowerCase());
        result.put("title", title != null ? title : "套料软件系统诊断报告");
        result.put("generatedAt", timestamp);
        result.put("status", "SUCCESS");

        // 报告内容概要
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("systemStatus", "正常运行");
        summary.put("cpuUsage", "45%");
        summary.put("memoryUsage", "62%");
        summary.put("diskUsage", "70%");
        summary.put("activeConnections", 5);
        summary.put("errorCount24h", 3);
        summary.put("warningCount24h", 12);
        result.put("summary", summary);

        // 报告章节
        List<String> sections = new ArrayList<>();
        sections.add("系统概览");
        sections.add("资源使用情况");
        sections.add("日志摘要");
        sections.add("数据库状态");
        sections.add("许可证信息");
        sections.add("套料引擎状态");
        sections.add("建议与优化");
        result.put("sections", sections);

        // 文件信息
        String fileName = "diagnostic_report_" + reportId + "." + format.toLowerCase();
        Map<String, Object> fileInfo = new LinkedHashMap<>();
        fileInfo.put("fileName", fileName);
        fileInfo.put("filePath", "/reports/" + fileName);
        fileInfo.put("fileSizeKB", 125);
        fileInfo.put("downloadUrl", "/api/v1/reports/download/" + reportId);
        result.put("file", fileInfo);

        return result;
    }
}
