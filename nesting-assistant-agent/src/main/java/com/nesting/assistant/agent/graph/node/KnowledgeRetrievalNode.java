package com.nesting.assistant.agent.graph.node;

import com.nesting.assistant.agent.graph.GraphNode;
import com.nesting.assistant.agent.graph.GraphState;
import com.nesting.assistant.domain.enums.AgentRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component("graphKnowledgeRetrievalNode")
public class KnowledgeRetrievalNode implements GraphNode {

    private static final String SYSTEM_PROMPT = """
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

    private final ChatClient chatClient;

    public KnowledgeRetrievalNode(ChatClient.Builder chatClientBuilder,
                                  ToolCallbackProvider knowledgeToolCallbackProvider) {
        ToolCallback[] tools = knowledgeToolCallbackProvider.getToolCallbacks();
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(tools)
                .build();
    }

    @Override
    public String getName() { return "knowledge_retrieval"; }

    @Override
    public GraphState process(GraphState state) {
        log.info("KnowledgeRetrievalNode processing: {}", state.getUserMessage());
        String response = chatClient.prompt()
                .user(state.getUserMessage())
                .call()
                .content();
        state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                .nodeName(getName()).role(AgentRole.KNOWLEDGE_RETRIEVAL)
                .inputSummary(state.getUserMessage()).outputSummary(response)
                .success(true).build());
        return state;
    }
}