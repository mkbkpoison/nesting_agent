package com.nesting.assistant.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * ChatClient配置
 * 提供全局 ChatClient Bean（含 RAG 和工具），供非 Agent 场景使用
 */
@Configuration
public class AgentConfig {

    @Value("${nesting.assistant.system-prompt}")
    private String systemPrompt;

    @Value("${nesting.assistant.rag.top-k:10}")
    private int defaultTopK;

    @Value("${nesting.assistant.rag.similarity-threshold:0.7}")
    private double defaultSimilarityThreshold;

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 VectorStore vectorStore,
                                 List<ToolCallbackProvider> callbackProviders) {

        // 合并所有 ToolCallbackProvider 中的 ToolCallback
        ToolCallback[] allCallbacks = callbackProviders.stream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .toArray(ToolCallback[]::new);

        // 创建 SearchRequest（直接 new，不作为 Bean 注入）
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(defaultTopK)
                .similarityThreshold(defaultSimilarityThreshold)
                .build();


        // 使用 Builder 方式（如果版本支持）
         QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                 .searchRequest(searchRequest)
                 .build();

        return chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(qaAdvisor)
                .defaultToolCallbacks(allCallbacks)
                .build();
    }
}