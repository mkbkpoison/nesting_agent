package com.nesting.assistant.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 用户消息
     */
    private String message;

    /**
     * 会话ID（可选，首次请求可为空）
     */
    private String conversationId;

    /**
     * 用户ID（可选）
     */
    private String userId;

    /**
     * 自定义系统提示词（可选）
     */
    private String systemPrompt;
}
