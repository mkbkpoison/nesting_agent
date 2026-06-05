package com.nesting.assistant.agent.impl;

import com.nesting.assistant.agent.core.*;
import com.nesting.assistant.domain.enums.AgentRole;
import com.nesting.assistant.domain.enums.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 知识检索智能体
 * 负责RAG知识库检索、相似问题匹配、解决方案推荐
 * 支持Tool Calling（调用KnowledgeSearchTool, ErrorCodeSolutionTool, SimilarCaseTool等）
 */
@Slf4j
@Component
public class KnowledgeRetrievalAgent extends BaseAgent {

    private final ChatClient chatClient;

    private static final String KNOWLEDGE_PROMPT = """
            你是套料软件的知识助手，帮助用户查询和理解软件使用文档。

            知识库内容包括：
            1. 用户手册：软件功能说明、操作指南
            2. FAQ：常见问题及解答
            3. 错误代码表：错误代码含义及解决方案
            4. 最佳实践：套料优化技巧、使用建议
            5. 配置指南：参数配置说明

            回答原则：
            - 优先使用知识库中的信息回答问题
            - 如果知识库中没有相关信息，如实告知
            - 提供信息时注明来源（如"根据用户手册..."）
            - 对于复杂问题，提供详细的操作步骤

            可用的知识检索工具：
            - searchKnowledgeBase: 搜索知识库
            - getRelatedSolutions: 获取错误代码解决方案
            - searchSimilarCases: 搜索相似案例

            请根据用户的问题选择合适的工具进行调用。
            """;

    public KnowledgeRetrievalAgent(ChatClient.Builder chatClientBuilder,
                                    ToolCallbackProvider knowledgeToolCallbackProvider) {
        super(AgentRole.KNOWLEDGE_RETRIEVAL, KNOWLEDGE_PROMPT);
        ToolCallback[] tools = knowledgeToolCallbackProvider.getToolCallbacks();
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(tools)
                .defaultSystem(KNOWLEDGE_PROMPT)
                .build();
    }

    @Override
    public AgentResponse process(AgentContext context) {
        log.info("KnowledgeRetrievalAgent processing: {}", context.getUserMessage());

        String response = chatClient.prompt()
                .system(KNOWLEDGE_PROMPT)
                .user(context.getUserMessage())
                .call()
                .content();

        return AgentResponse.builder()
                .content(response)
                .agentRole(AgentRole.KNOWLEDGE_RETRIEVAL)
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public boolean canHandle(AgentContext context) {
        if (context.getIntent() == null) {
            return false;
        }
        return context.getIntent().getIntentType() == IntentType.KNOWLEDGE_QUERY;
    }
}
