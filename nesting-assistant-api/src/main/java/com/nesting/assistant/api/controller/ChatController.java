package com.nesting.assistant.api.controller;

import com.nesting.assistant.agent.model.ChatRequest;
import com.nesting.assistant.agent.model.ChatResponse;
import com.nesting.assistant.agent.service.NestingAssistantService;
import com.nesting.assistant.api.auth.TokenInterceptor;
import com.nesting.assistant.common.model.ApiResponse;
import com.nesting.assistant.common.util.IdGenerator;
import com.nesting.assistant.domain.entity.Conversation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天控制器
 * userId 通过 TokenInterceptor 自动从 Authorization header 获取
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "聊天接口", description = "套料软件智能助手聊天API，支持多用户多对话管理")
public class ChatController {

    private final NestingAssistantService assistantService;

    private String getUserId(HttpServletRequest request) {
        return TokenInterceptor.getCurrentUserId(request);
    }

    // ==================== 消息接口 ====================

    @PostMapping
    @Operation(summary = "发送消息", description = "发送消息给智能助手并获取响应")
    public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request, HttpServletRequest servletRequest) {
        String userId = getUserId(servletRequest);
        request.setUserId(userId);
        log.info("Received chat request: userId={}, message={}", userId, request.getMessage());
        ChatResponse response = assistantService.chat(request);
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天", description = "发送消息并以SSE流式方式接收响应")
    public SseEmitter streamChat(@RequestBody ChatRequest request, HttpServletRequest servletRequest) {
        String userId = getUserId(servletRequest);
        request.setUserId(userId);
        log.info("Received streaming chat request: userId={}, message={}", userId, request.getMessage());

        SseEmitter emitter = new SseEmitter(300_000L);

        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = IdGenerator.conversationId();
        }
        final String finalConversationId = conversationId;

        // 先发送会话ID事件
        try {
            emitter.send(SseEmitter.event()
                    .name("meta")
                    .data(Map.of("conversationId", finalConversationId)));
        } catch (Exception e) {
            log.error("Error sending meta event", e);
            emitter.completeWithError(e);
            return emitter;
        }

        // 获取流式响应
        Flux<String> stream = assistantService.streamChat(
                ChatRequest.builder()
                        .message(request.getMessage())
                        .conversationId(finalConversationId)
                        .userId(userId)
                        .build()
        );

        stream.subscribe(
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(Map.of("content", chunk)));
                    } catch (Exception e) {
                        log.error("Error sending SSE event", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("Stream error", error);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(Map.of("error", error.getMessage())));
                    } catch (Exception e) {
                        // ignore
                    }
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("Stream completed for conversation: {}", finalConversationId);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data(Map.of("conversationId", finalConversationId)));
                    } catch (Exception e) {
                        // ignore
                    }
                    emitter.complete();
                }
        );

        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout for conversation: {}", finalConversationId);
            emitter.complete();
        });

        emitter.onError(e -> {
            log.error("SSE connection error for conversation: {}", finalConversationId, e);
        });

        return emitter;
    }

    // ==================== 对话管理接口 ====================

    @GetMapping("/conversations")
    @Operation(summary = "获取对话列表", description = "获取当前用户的所有对话列表")
    public ApiResponse<List<Map<String, Object>>> getConversations(HttpServletRequest request) {
        String userId = getUserId(request);
        List<Conversation> convs = assistantService.getUserConversations(userId);

        List<Map<String, Object>> result = convs.stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("conversationId", c.getConversationId());
                    m.put("title", c.getTitle());
                    m.put("messageCount", c.getMessageCount());
                    m.put("status", c.getStatus());
                    m.put("createdAt", c.getCreatedAt());
                    m.put("updatedAt", c.getUpdatedAt());
                    return m;
                })
                .collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    @GetMapping("/history/{conversationId}")
    @Operation(summary = "获取历史", description = "获取指定会话的聊天历史")
    public ApiResponse<List<Map<String, Object>>> getHistory(
            @PathVariable String conversationId,
            HttpServletRequest request) {
        String userId = getUserId(request);
        var messages = assistantService.getHistory(userId, conversationId);

        List<Map<String, Object>> history = messages.stream()
                .map(msg -> Map.<String, Object>of(
                        "role", msg.getMessageType().getValue(),
                        "content", msg.getText()
                ))
                .toList();

        return ApiResponse.success(history);
    }

    @DeleteMapping("/clear/{conversationId}")
    @Operation(summary = "清除会话", description = "清除指定会话的所有历史记录")
    public ApiResponse<Void> clearConversation(
            @PathVariable String conversationId,
            HttpServletRequest request) {
        String userId = getUserId(request);
        assistantService.clearConversation(userId, conversationId);
        return ApiResponse.success("会话已清除", null);
    }

    @PutMapping("/rename/{conversationId}")
    @Operation(summary = "重命名会话", description = "重命名指定会话")
    public ApiResponse<Void> renameConversation(
            @PathVariable String conversationId,
            @RequestParam String title,
            HttpServletRequest request) {
        String userId = getUserId(request);
        assistantService.renameConversation(userId, conversationId, title);
        return ApiResponse.success("会话已重命名", null);
    }
}
