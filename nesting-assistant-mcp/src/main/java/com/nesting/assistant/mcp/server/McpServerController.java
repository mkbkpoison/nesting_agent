package com.nesting.assistant.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * MCP Server控制器
 * 基于JSON-RPC 2.0协议，提供 tools/list 和 tools/call 端点。
 * 与 McpHttpClient 客户端兼容。
 */
@Slf4j
@RestController
@RequestMapping("${nesting.mcp.server.base-path:/mcp}")
public class McpServerController {

    private final Map<String, ToolCallback> toolRegistry;
    private final ObjectMapper objectMapper;

    @Value("${nesting.mcp.server.name:nesting-mcp-server}")
    private String serverName;

    @Value("${nesting.mcp.server.version:1.0.0}")
    private String version;

    public McpServerController(Map<String, ToolCallback> mcpToolRegistry, ObjectMapper objectMapper) {
        this.toolRegistry = mcpToolRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * JSON-RPC 2.0 统一入口
     * 处理 tools/list 和 tools/call 方法
     */
    @PostMapping
    public Map<String, Object> handleJsonRpc(@RequestBody Map<String, Object> request) {
        String id = Objects.toString(request.getOrDefault("id", UUID.randomUUID().toString()));
        String method = (String) request.get("method");

        log.debug("MCP request: method={}, id={}", method, id);

        try {
            return switch (method) {
                case "tools/list" -> handleListTools(id);
                case "tools/call" -> handleCallTool(id, request);
                default -> createErrorResponse(id, -32601, "Method not found: " + method);
            };
        } catch (Exception e) {
            log.error("MCP request failed: method={}, id={}", method, id, e);
            return createErrorResponse(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * 处理 tools/list - 返回所有可用工具列表
     */
    private Map<String, Object> handleListTools(String id) {
        List<Map<String, Object>> tools = new ArrayList<>();

        for (Map.Entry<String, ToolCallback> entry : toolRegistry.entrySet()) {
            ToolDefinition def = entry.getValue().getToolDefinition();
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("name", def.name());
            tool.put("description", def.description());
            tool.put("inputSchema", def.inputSchema() != null ? def.inputSchema() : Map.of("type", "object", "properties", Map.of()));
            tools.add(tool);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverInfo", Map.of(
                "name", serverName,
                "version", version,
                "protocolVersion", "2025-03-26"
        ));
        result.put("tools", tools);
        result.put("totalTools", tools.size());

        return createSuccessResponse(id, result);
    }

    /**
     * 处理 tools/call - 调用指定工具
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> handleCallTool(String id, Map<String, Object> request) {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        if (params == null) {
            return createErrorResponse(id, -32602, "Invalid params: missing 'params'");
        }

        String toolName = (String) params.get("name");
        if (toolName == null || toolName.isEmpty()) {
            return createErrorResponse(id, -32602, "Invalid params: missing 'name'");
        }

        ToolCallback callback = toolRegistry.get(toolName);
        if (callback == null) {
            return createErrorResponse(id, -32602, "Tool not found: " + toolName);
        }

        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", new HashMap<>());

        log.info("MCP calling tool: {} with arguments: {}", toolName, arguments);

        try {
            // ToolCallback.call() 接收 JSON 字符串参数，返回 JSON 字符串结果
            String argsJson = objectMapper.writeValueAsString(arguments);
            String resultJson = callback.call(argsJson);

            // 解析 JSON 结果为 Map
            Object result;
            try {
                result = objectMapper.readValue(resultJson, Map.class);
            } catch (Exception e) {
                result = resultJson;
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("toolResult", result);
            response.put("isError", false);

            return createSuccessResponse(id, response);
        } catch (Exception e) {
            log.error("MCP tool call failed: {}", toolName, e);
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("toolResult", Map.of("error", e.getMessage()));
            errorResult.put("isError", true);
            return createSuccessResponse(id, errorResult);
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("server", serverName);
        info.put("version", version);
        info.put("registeredTools", toolRegistry.size());
        info.put("tools", toolRegistry.keySet());
        return info;
    }

    private Map<String, Object> createSuccessResponse(String id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> createErrorResponse(String id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }
}
