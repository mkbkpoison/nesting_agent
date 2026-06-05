package com.nesting.assistant.agent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 智能体响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 处理该请求的智能体角色
     */
    private com.nesting.assistant.domain.enums.AgentRole agentRole;

    /**
     * 意图类型
     */
    private String intentType;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 调用的工具列表
     */
    private List<String> toolsCalled;

    /**
     * 是否需要继续处理
     */
    private boolean needContinue;

    /**
     * 下一个处理的智能体
     */
    private com.nesting.assistant.domain.enums.AgentRole nextAgent;

    /**
     * 结构化数据
     */
    private Map<String, Object> data;

    /**
     * 推荐操作
     */
    private List<String> recommendations;

    /**
     * 置信度
     */
    private double confidence;

    /**
     * 响应时间
     */
    private LocalDateTime timestamp;

    public static AgentResponse of(String content) {
        return AgentResponse.builder()
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static AgentResponse of(String content, com.nesting.assistant.domain.enums.AgentRole agentRole) {
        return AgentResponse.builder()
                .content(content)
                .agentRole(agentRole)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
