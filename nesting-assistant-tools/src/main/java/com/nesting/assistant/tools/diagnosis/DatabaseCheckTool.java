package com.nesting.assistant.tools.diagnosis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;

/**
 * 数据库连接检查工具
 */
@Slf4j
@Service
public class DatabaseCheckTool {

    private final DataSource dataSource;

    public DatabaseCheckTool(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Tool(description = "检查数据库连接状态，测试连接是否正常。" +
            "返回数据库类型、版本、连接池状态和响应时间。" +
            "当用户报告数据库相关问题或需要验证数据库状态时使用此工具。")
    public Map<String, Object> checkDatabaseConnection() {
        log.info("Checking database connection status");

        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            result.put("status", "CONNECTED");
            result.put("database", meta.getDatabaseProductName());
            result.put("version", meta.getDatabaseProductVersion());
            result.put("url", meta.getURL());
            result.put("username", meta.getUserName());
            result.put("responseTimeMs", System.currentTimeMillis() - startTime);
            result.put("isValid", conn.isValid(5));
            result.put("autoCommit", conn.getAutoCommit());
            result.put("readOnly", conn.isReadOnly());

            // 连接池状态（如果使用的是 HikariCP）
            Map<String, Object> poolStats = new LinkedHashMap<>();
            try {
                if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikariDS) {
                    poolStats.put("activeConnections", hikariDS.getHikariPoolMXBean().getActiveConnections());
                    poolStats.put("idleConnections", hikariDS.getHikariPoolMXBean().getIdleConnections());
                    poolStats.put("maxConnections", hikariDS.getMaximumPoolSize());
                    poolStats.put("pendingRequests", hikariDS.getHikariPoolMXBean().getThreadsAwaitingConnection());
                } else {
                    poolStats.put("note", "Not using HikariCP, pool metrics unavailable");
                }
            } catch (Exception e) {
                poolStats.put("note", "Pool metrics unavailable: " + e.getMessage());
            }
            result.put("poolStats", poolStats);

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            result.put("responseTimeMs", System.currentTimeMillis() - startTime);
            log.error("Database connection check failed", e);
        }

        return result;
    }
}
