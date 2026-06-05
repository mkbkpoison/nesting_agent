package com.nesting.assistant.agent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;

/**
 * 智能体上下文
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户消息
     */
    private String userMessage;

    /**
     * 对话历史
     */
    private List<Message> history;

    /**
     * 当前意图
     */
    private AgentIntent intent;

    /**
     * 智能体间传递的数据
     */
    private Map<String, Object> sharedData;

    /**
     * 当前处理的智能体角色
     */
    private com.nesting.assistant.domain.enums.AgentRole currentAgent;

    /**
     * 工具调用结果
     */
    private List<ToolCallResult> toolCallResults;

    /**
     * 是否需要人工介入
     */
    private boolean needHumanIntervention;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    public void addSharedData(String key, Object value) {
        if (sharedData == null) {
            sharedData = new java.util.HashMap<>();
        }
        sharedData.put(key, value);
    }

    public Object getSharedData(String key) {
        return sharedData != null ? sharedData.get(key) : null;
    }
}
