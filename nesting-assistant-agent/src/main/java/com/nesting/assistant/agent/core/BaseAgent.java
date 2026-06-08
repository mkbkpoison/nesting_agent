package com.nesting.assistant.agent.core;

import com.nesting.assistant.domain.enums.AgentRole;

/**
 * 智能体抽象基类
 */
public abstract class BaseAgent {

    protected final AgentRole role;
    protected final String systemPrompt;

    protected BaseAgent(AgentRole role, String systemPrompt) {
        this.role = role;
        this.systemPrompt = systemPrompt;
    }

    /**
     * 处理请求
     */
    public abstract AgentResponse process(AgentContext context);

    /**
     * 判断是否能处理该请求
     */
    public abstract boolean canHandle(AgentContext context);

    /**
     * 获取智能体角色
     */
    public AgentRole getRole() {
        return role;
    }

    /**
     * 获取系统提示词
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 构建一个简单的上下文（供委派工具使用）
     */
    public AgentContext buildContext(String userMessage) {
        return AgentContext.builder()
                .userMessage(userMessage)
                .sharedData(new java.util.HashMap<>())
                .toolCallResults(new java.util.ArrayList<>())
                .build();
    }

    /**
     * 构建增强提示词
     */
    protected String buildEnhancedPrompt(AgentContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(systemPrompt).append("\n\n");

        if (context.getIntent() != null) {
            prompt.append("用户意图: ").append(context.getIntent().getIntentType().getName()).append("\n");
        }

        if (context.getSharedData() != null && !context.getSharedData().isEmpty()) {
            prompt.append("上下文数据:\n");
            context.getSharedData().forEach((k, v) ->
                    prompt.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        return prompt.toString();
    }
}
