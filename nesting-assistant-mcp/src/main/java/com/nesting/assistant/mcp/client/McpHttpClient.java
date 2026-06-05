package com.nesting.assistant.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MCP HTTP客户端
 * 用于与外部MCP服务器通信
 */
@Slf4j
@Component
public class McpHttpClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String serverUrl;
    private final String apiKey;

    public McpHttpClient(
            @Value("${nesting.mcp.client.server-url:}") String serverUrl,
            @Value("${nesting.mcp.client.api-key:}") String apiKey) {
        this.serverUrl = serverUrl;
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用远程工具
     */
    public Object callTool(String toolName, Map<String, Object> arguments) throws IOException {
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalStateException("MCP server URL not configured");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments != null ? arguments : new HashMap<>());

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", UUID.randomUUID().toString());
        request.put("method", "tools/call");
        request.put("params", params);

        String requestBody = objectMapper.writeValueAsString(request);
        log.debug("MCP request: {}", requestBody);

        Request httpRequest = new Request.Builder()
                .url(serverUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("MCP request failed: " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("MCP response: {}", responseBody);

            Map<String, Object> result = objectMapper.readValue(responseBody, HashMap.class);
            if (result.containsKey("error")) {
                throw new RuntimeException("MCP error: " + result.get("error"));
            }

            return result.get("result");
        }
    }

    /**
     * 列出远程可用工具
     */
    public Object listTools() throws IOException {
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalStateException("MCP server URL not configured");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", UUID.randomUUID().toString());
        request.put("method", "tools/list");

        String requestBody = objectMapper.writeValueAsString(request);

        Request httpRequest = new Request.Builder()
                .url(serverUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("MCP request failed: " + response.code());
            }

            String responseBody = response.body().string();
            Map<String, Object> result = objectMapper.readValue(responseBody, HashMap.class);
            return result.get("result");
        }
    }
}
