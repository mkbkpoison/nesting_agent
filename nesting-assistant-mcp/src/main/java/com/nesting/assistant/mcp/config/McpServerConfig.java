package com.nesting.assistant.mcp.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

/**
 * MCP服务器配置
 * 将Spring AI @Tool注解的工具注册为MCP协议的可用工具，
 * 供外部MCP客户端通过JSON-RPC 2.0调用。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "nesting.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class McpServerConfig {

    @Value("${nesting.mcp.server.port:8081}")
    @Getter
    private int mcpPort;

    @Value("${nesting.mcp.server.name:nesting-mcp-server}")
    @Getter
    private String serverName;

    @Value("${nesting.mcp.server.version:1.0.0}")
    @Getter
    private String version;

    private final List<ToolCallbackProvider> toolCallbackProviders;

    public McpServerConfig(List<ToolCallbackProvider> toolCallbackProviders) {
        this.toolCallbackProviders = toolCallbackProviders;
    }

    /**
     * 将所有工具的ToolCallback合并为一个注册表Map
     * 注：McpToolRegistry 组件也提供了类似功能，两者二选一
     */
    @Bean
    public Map<String, ToolCallback> mcpToolCallbackMap() {
        Map<String, ToolCallback> registry = new LinkedHashMap<>();

        for (ToolCallbackProvider provider : toolCallbackProviders) {
            for (ToolCallback callback : provider.getToolCallbacks()) {
                ToolDefinition def = callback.getToolDefinition();
                registry.put(def.name(), callback);
                log.debug("Registered MCP tool: {} - {}", def.name(), def.description());
            }
        }

        log.info("MCP Server initialized: name={}, version={}, tools={}",
                serverName, version, registry.size());
        return Collections.unmodifiableMap(registry);
    }
}
