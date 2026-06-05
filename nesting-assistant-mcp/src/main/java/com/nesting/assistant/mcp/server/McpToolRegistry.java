package com.nesting.assistant.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP工具注册表
 * 汇总所有可用工具并支持通过MCP协议暴露
 */
@Slf4j
@Component
public class McpToolRegistry {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, ToolCallback> tools;

    @Autowired
    public McpToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取所有已注册的工具
     * 从Spring容器中获取所有ToolCallback类型的Bean
     */
    public Map<String, ToolCallback> getTools() {
        if (tools == null) {
            tools = new HashMap<>();
            // 从Spring容器中获取所有ToolCallback类型的Bean
            Map<String, ToolCallback> callbackBeans =
                    applicationContext.getBeansOfType(ToolCallback.class);

            for (Map.Entry<String, ToolCallback> entry : callbackBeans.entrySet()) {
                ToolCallback callback = entry.getValue();
                // 通过 ToolDefinition 获取工具名称
                String toolName = callback.getToolDefinition().name();
                tools.put(toolName, callback);
                log.debug("Registered MCP tool: {}", toolName);
            }
            log.info("Registered {} MCP tools", tools.size());
        }
        return tools;
    }

    /**
     * 列出所有工具的Schema
     */
    public List<Map<String, Object>> listTools() {
        List<Map<String, Object>> toolList = new ArrayList<>();
        for (ToolCallback tool : getTools().values()) {
            ToolDefinition definition = tool.getToolDefinition();
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("name", definition.name());
            schema.put("description", definition.description());
            schema.put("inputSchema", definition.inputSchema());
            toolList.add(schema);
        }
        return toolList;
    }

    /**
     * 执行工具
     */
    public Object executeTool(String name, Map<String, Object> arguments) {
        ToolCallback tool = getTools().get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found: " + name);
        }

        try {
            String argsJson = objectMapper.writeValueAsString(arguments);
            String result = tool.call(argsJson);
            log.info("Executed tool: {} with arguments: {}", name, arguments);

            // 尝试解析返回结果
            if (result != null && !result.isEmpty()) {
                try {
                    return objectMapper.readValue(result, Object.class);
                } catch (Exception e) {
                    // 如果不是JSON，直接返回字符串
                    return result;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to execute tool: {}", name, e);
            throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return getTools().containsKey(name);
    }

    /**
     * 获取工具信息
     */
    public Map<String, Object> getToolInfo(String name) {
        ToolCallback tool = getTools().get(name);
        if (tool == null) {
            return null;
        }
        ToolDefinition definition = tool.getToolDefinition();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", definition.name());
        info.put("description", definition.description());
        info.put("inputSchema", definition.inputSchema());
        return info;
    }
}