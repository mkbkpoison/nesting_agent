package com.nesting.assistant.tools.diagnosis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NestingEngineTool {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_ENGINE = "nesting:engine";

    @Tool(description = "检查套料引擎运行状态，包括引擎版本、当前任务、计算资源等信息。" +
            "当套料功能异常或需要了解引擎状态时使用此工具。")
    public Map<String, Object> checkNestingEngineStatus() {
        log.info("Checking nesting engine status");

        Map<String, Object> result = new LinkedHashMap<>();

        // 引擎基本信息
        Map<String, Object> engine = new LinkedHashMap<>();
        engine.put("name", "NestingEngine Pro");
        engine.put("version", "3.2.1");
        engine.put("status", "RUNNING");
        engine.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        result.put("engine", engine);

        // 从 Redis 读取引擎任务状态
        String taskData = redisTemplate.opsForValue().get(KEY_ENGINE);
        List<Map<String, Object>> tasks = new ArrayList<>();

        if (taskData != null && !taskData.isEmpty()) {
            String[] taskLines = taskData.split("\n");
            for (String line : taskLines) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    Map<String, Object> task = new LinkedHashMap<>();
                    task.put("taskId", parts[0].trim());
                    task.put("type", parts[1].trim());
                    task.put("status", parts[2].trim());
                    task.put("progress", parts[3].trim() + "%");
                    task.put("description", parts.length > 4 ? parts[4].trim() : "");
                    task.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    tasks.add(task);
                }
            }
        }

        result.put("taskQueue", tasks);
        result.put("activeTaskCount", tasks.stream().filter(t -> "RUNNING".equals(t.get("status"))).count());
        result.put("pendingTaskCount", tasks.stream().filter(t -> "PENDING".equals(t.get("status"))).count());

        // 计算资源（JVM 线程池信息）
        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("maxWorkers", Runtime.getRuntime().availableProcessors());
        resources.put("activeWorkers", ManagementFactory.getThreadMXBean().getThreadCount());
        resources.put("idleWorkers", Math.max(0, Runtime.getRuntime().availableProcessors() - ManagementFactory.getThreadMXBean().getThreadCount()));
        resources.put("queueCapacity", 100);
        resources.put("currentQueueSize", tasks.size());
        result.put("resources", resources);

        // 性能统计
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalNestingJobs", tasks.size());
        stats.put("averageUtilization", "N/A (need historical data)");
        stats.put("averageProcessingTime", "N/A (need historical data)");
        stats.put("successRate", "N/A (need historical data)");
        stats.put("errors", tasks.stream().filter(t -> "FAILED".equals(t.get("status"))).count());
        result.put("statistics", stats);

        // 健康检查
        List<String> issues = new ArrayList<>();
        if ((long) resources.get("activeWorkers") > Runtime.getRuntime().availableProcessors() * 2) {
            issues.add("线程数过高，可能导致系统负载上升");
        }
        result.put("issues", issues);
        result.put("healthy", issues.isEmpty());

        return result;
    }
}