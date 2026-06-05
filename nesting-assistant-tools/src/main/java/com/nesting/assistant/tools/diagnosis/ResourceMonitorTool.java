package com.nesting.assistant.tools.diagnosis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.*;

/**
 * 系统资源监控工具
 */
@Slf4j
@Service
public class ResourceMonitorTool {

    @Tool(description = "获取系统资源使用情况，包括CPU、内存、磁盘使用率等。" +
            "当系统运行缓慢或需要评估硬件资源时使用此工具。")
    public Map<String, Object> getSystemResources() {
        log.info("Getting system resources");

        Map<String, Object> result = new LinkedHashMap<>();

        // 内存信息
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("heapUsedMB", heapUsed / (1024 * 1024));
        memory.put("heapMaxMB", heapMax / (1024 * 1024));
        memory.put("heapUsedPercent", String.format("%.1f%%", (double) heapUsed / heapMax * 100));
        memory.put("nonHeapUsedMB", nonHeapUsed / (1024 * 1024));
        result.put("memory", memory);

        // CPU信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("availableProcessors", osBean.getAvailableProcessors());
        cpu.put("systemLoadAverage", osBean.getSystemLoadAverage());
        cpu.put("architecture", osBean.getArch());
        cpu.put("osName", osBean.getName());
        cpu.put("osVersion", osBean.getVersion());
        result.put("cpu", cpu);

        // 磁盘信息（模拟）
        Map<String, Object> disk = new LinkedHashMap<>();
        disk.put("totalSpaceGB", 500);
        disk.put("freeSpaceGB", 150);
        disk.put("usedSpaceGB", 350);
        disk.put("usedPercent", "70%");
        result.put("disk", disk);

        // JVM信息
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("name", ManagementFactory.getRuntimeMXBean().getVmName());
        jvm.put("version", System.getProperty("java.version"));
        jvm.put("vendor", System.getProperty("java.vendor"));
        jvm.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        result.put("jvm", jvm);

        // 线程信息
        Map<String, Object> threads = new LinkedHashMap<>();
        threads.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        threads.put("peakThreadCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
        threads.put("daemonThreadCount", ManagementFactory.getThreadMXBean().getDaemonThreadCount());
        result.put("threads", threads);

        // 健康状态评估
        List<String> warnings = new ArrayList<>();
        double heapUsagePercent = (double) heapUsed / heapMax * 100;
        if (heapUsagePercent > 80) {
            warnings.add("JVM堆内存使用率超过80%，建议增加内存或优化应用");
        }
        if (osBean.getSystemLoadAverage() > osBean.getAvailableProcessors()) {
            warnings.add("系统负载较高，可能影响性能");
        }
        result.put("warnings", warnings);
        result.put("healthy", warnings.isEmpty());

        return result;
    }
}
