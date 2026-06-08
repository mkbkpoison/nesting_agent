package com.nesting.assistant.agent.service;

import com.nesting.assistant.agent.core.AgentResponse;
import com.nesting.assistant.agent.model.ChatRequest;
import com.nesting.assistant.agent.model.ChatResponse;
import com.nesting.assistant.agent.orchestration.AgentOrchestrator;
import com.nesting.assistant.domain.entity.Conversation;
import com.nesting.assistant.memory.manager.ConversationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

/**
 * 套料助手服务
 * 对外提供的主要服务接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NestingAssistantService {

    private final AgentOrchestrator orchestrator;
    private final ConversationManager conversationManager;

    /**
     * 处理聊天请求
     */
    public ChatResponse chat(ChatRequest request) {
        log.info("Processing chat request: userId={}, message={}", request.getUserId(), request.getMessage());

        // 创建或获取会话
        String conversationId = conversationManager.createOrGetConversation(
                request.getUserId(),
                request.getConversationId(),
                request.getMessage()
        );

        // 保存用户消息
        conversationManager.addUserMessage(request.getUserId(), conversationId, request.getMessage());

        // 编排处理
        AgentResponse response = orchestrator.orchestrate(
                conversationId,
                request.getUserId(),
                request.getMessage()
        );

        // 保存助手响应
        conversationManager.addAssistantMessage(
                request.getUserId(), conversationId, response.getContent(),
                response.getAgentRole() != null ? response.getAgentRole().getCode() : null,
                response.getIntentType()
        );

        // 刷新TTL
        conversationManager.refreshTtl(request.getUserId(), conversationId);

        return ChatResponse.builder()
                .message(response.getContent())
                .conversationId(conversationId)
                .agentRole(response.getAgentRole() != null ? response.getAgentRole().getCode() : null)
                .intentType(response.getIntentType())
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * 流式聊天响应 — 通过 Sinks.Many + 后台线程实现真正的 LLM Token 级流式推送。
     */
    public Flux<String> streamChat(ChatRequest request) {
        log.info("Processing streaming chat request: userId={}, message={}",
                request.getUserId(), request.getMessage());

        String userId = request.getUserId();
        String message = request.getMessage();

        // 创建或获取会话
        String conversationId = conversationManager.createOrGetConversation(
                userId, request.getConversationId(), message);
        final String finalConversationId = conversationId;

        // 保存用户消息
        conversationManager.addUserMessage(userId, finalConversationId, message);

        // 创建流式 Sink
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 后台线程执行编排，实时推送 Token
        Thread executorThread = new Thread(() -> {
            try {
                AgentResponse agentResponse = orchestrator.orchestrate(
                        finalConversationId, userId, message,
                        token -> sink.tryEmitNext(token));

                // 保存助手响应
                conversationManager.addAssistantMessage(
                        userId, finalConversationId, agentResponse.getContent(),
                        agentResponse.getAgentRole() != null ? agentResponse.getAgentRole().getCode() : null,
                        agentResponse.getIntentType());

                conversationManager.refreshTtl(userId, finalConversationId);
                sink.tryEmitComplete();
            } catch (Exception e) {
                log.error("Streaming execution failed", e);
                sink.tryEmitError(e);
            }
        }, "stream-chat-" + finalConversationId);
        executorThread.start();

        return sink.asFlux();
    }

    /**
     * 获取用户的对话列表
     */
    public List<Conversation> getUserConversations(String userId) {
        return conversationManager.getUserConversations(userId);
    }

    /**
     * 获取指定会话的历史
     */
    public List<Message> getHistory(String userId, String conversationId) {
        return conversationManager.getConversationHistory(userId, conversationId);
    }

    /**
     * 清除会话
     */
    public void clearConversation(String userId, String conversationId) {
        conversationManager.deleteConversation(userId, conversationId);
    }

    /**
     * 重命名会话
     */
    public void renameConversation(String userId, String conversationId, String newTitle) {
        conversationManager.renameConversation(userId, conversationId, newTitle);
    }
}
