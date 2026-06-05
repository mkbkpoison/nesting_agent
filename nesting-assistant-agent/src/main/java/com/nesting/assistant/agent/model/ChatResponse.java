package com.nesting.assistant.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 响应消息
     */
    private String message;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 处理的智能体角色
     */
    private String agentRole;

    /**
     * 意图类型
     */
    private String intentType;

    /**
     * 响应时间
     */
    private LocalDateTime timestamp;

    public static ChatResponse of(String message, String conversationId) {
        return ChatResponse.builder()
                .message(message)
                .conversationId(conversationId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
