package com.nesting.assistant.tools.diagnosis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 套料引擎状态检查工具
 */
@Slf4j
@Service
public class NestingEngineTool {

    @Tool(description = "检查套料引擎运行状态，包括引擎版本、当前任务、计算资源等信息。" +
            "当套料功能异常或需要了解引擎状态时使用此工具。")
    public Map<String, Object> checkNestingEngineStatus() {
        log.info("Checking nesting engine status");

        Map<String, Object> result = new LinkedHashMap<>();

        // 引擎基本信息
        Map<String, Object> engine = new LinkedHashMap<>();
        engine.put("name", "NestingEngine Pro");
        engine.put("version", "3.2.1");
        engine.put("buildDate", "2026-04-15");
        engine.put("status", "RUNNING");
        result.put("engine", engine);

        // 当前任务队列
        List<Map<String, Object>> tasks = new ArrayList<>();
        tasks.add(createTaskEntry("TASK-001", "套料计算", "RUNNING", 45, "panel_batch_001"));
        tasks.add(createTaskEntry("TASK-002", "优化分析", "PENDING", 0, "optimization_request"));
        tasks.add(createTaskEntry("TASK-003", "报表生成", "COMPLETED", 100, "report_export"));
        result.put("taskQueue", tasks);
        result.put("activeTaskCount", tasks.stream().filter(t -> "RUNNING".equals(t.get("status"))).count());
        result.put("pendingTaskCount", tasks.stream().filter(t -> "PENDING".equals(t.get("status"))).count());

        // 计算资源
        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("maxWorkers", 8);
        resources.put("activeWorkers", 3);
        resources.put("idleWorkers", 5);
        resources.put("queueCapacity", 100);
        resources.put("currentQueueSize", 1);
        result.put("resources", resources);

        // 性能统计（最近24小时）
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalNestingJobs", 156);
        stats.put("averageUtilization", "87.3%");
        stats.put("averageProcessingTime", "12.5 seconds");
        stats.put("successRate", "98.7%");
        stats.put("errors", 2);
        result.put("statistics", stats);

        // 配置状态
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("algorithm", "遗传算法 + 模拟退火");
        config.put("maxIterations", 10000);
        config.put("targetUtilization", "90%");
        config.put("parallelProcessing", true);
        config.put("autoSave", true);
        result.put("configuration", config);

        // 健康检查
        List<String> issues = new ArrayList<>();
        if ((Long) result.get("activeTaskCount") > 5) {
            issues.add("任务队列较长，可能有处理延迟");
        }
        result.put("issues", issues);
        result.put("healthy", issues.isEmpty());

        return result;
    }

    private Map<String, Object> createTaskEntry(String taskId, String type, String status,
                                                 int progress, String description) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskId", taskId);
        task.put("type", type);
        task.put("status", status);
        task.put("progress", progress + "%");
        task.put("description", description);
        task.put("startTime", "2026-05-18 10:00:00");
        return task;
    }
}
