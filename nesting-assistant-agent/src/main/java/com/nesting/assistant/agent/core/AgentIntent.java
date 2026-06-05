package com.nesting.assistant.agent.core;

import com.nesting.assistant.domain.enums.IntentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能体意图识别结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentIntent {

    /**
     * 意图类型
     */
    private IntentType intentType;

    /**
     * 置信度 (0-1)
     */
    private double confidence;

    /**
     * 关键实体
     */
    private List<String> entities;

    /**
     * 关键词
     */
    private List<String> keywords;

    /**
     * 推荐处理的智能体
     */
    private List<com.nesting.assistant.domain.enums.AgentRole> recommendedAgents;

    /**
     * 推理过程
     */
    private String reasoning;

    /**
     * 是否需要工具调用
     */
    private boolean needsTools;

    /**
     * 推荐调用的工具
     */
    private List<String> recommendedTools;

    public static AgentIntent of(IntentType type, double confidence) {
        return AgentIntent.builder()
                .intentType(type)
                .confidence(confidence)
                .build();
    }
}
