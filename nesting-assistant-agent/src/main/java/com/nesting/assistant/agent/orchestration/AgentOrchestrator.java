package com.nesting.assistant.agent.orchestration;

import com.nesting.assistant.agent.core.AgentResponse;
import com.nesting.assistant.agent.graph.*;
import com.nesting.assistant.agent.graph.node.*;
import com.nesting.assistant.domain.enums.AgentRole;
import com.nesting.assistant.memory.manager.ConversationManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 智能体编排器。
 * 在 @PostConstruct 中组装 AgentGraph，运行时通过 GraphExecutor 驱动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final RouterNode routerNode;
    private final TechnicalSupportNode technicalSupportNode;
    private final SystemDiagnosisNode systemDiagnosisNode;
    private final KnowledgeRetrievalNode knowledgeRetrievalNode;
    private final OperationNode operationNode;
    private final ConversationManager conversationManager;

    private AgentGraph graph;
    private final GraphExecutor graphExecutor = new GraphExecutor();

    @PostConstruct
    public void initGraph() {
        graph = new AgentGraph()
                .setEntryPoint("router")
                .addNode("router", routerNode)
                .addNode("technical_support", technicalSupportNode)
                .addNode("system_diagnosis", systemDiagnosisNode)
                .addNode("knowledge_retrieval", knowledgeRetrievalNode)
                .addNode("operation", operationNode)
                .addConditionalEdge("router", state ->
                        state.getNextNode() != null ? state.getNextNode() : AgentGraph.END)
                .addEdge("technical_support", "router")
                .addEdge("system_diagnosis", "router")
                .addEdge("knowledge_retrieval", "router")
                .addEdge("operation", "router");
        log.info("AgentGraph initialized: 5 nodes, entry=router, 4 sub-nodes -> router loop");
    }

    public AgentResponse orchestrate(String conversationId, String userId, String userMessage) {
        log.info("Orchestrating: userId={}, conversationId={}, messageLen={}",
                userId, conversationId, userMessage != null ? userMessage.length() : 0);

        // 加载对话历史
        var history = conversationManager.getConversationHistory(
                userId != null ? userId : "default", conversationId);

        GraphState state = GraphState.builder()
                .conversationId(conversationId)
                .userId(userId != null ? userId : "default")
                .userMessage(userMessage)
                .history(history)
                .sharedData(new HashMap<>())
                .maxSteps(10)
                .build();

        state = graphExecutor.execute(graph, state);

        String content = buildFinalResponse(state);

        AgentResponse response = AgentResponse.builder()
                .content(content)
                .agentRole(AgentRole.ROUTER)
                .conversationId(conversationId)
                .intentType(detectIntentType(userMessage))
                .timestamp(java.time.LocalDateTime.now())
                .build();

        log.info("Orchestration complete: {} steps, contentLen={}",
                state.getStepCount(), content != null ? content.length() : 0);
        return response;
    }

    private String buildFinalResponse(GraphState state) {
        if (state.getFinalAnswer() != null && !state.getFinalAnswer().isBlank()) {
            return state.getFinalAnswer();
        }
        var expertOutputs = new ArrayList<String>();
        if (state.getExecutionHistory() != null) {
            for (GraphState.NodeExecutionRecord record : state.getExecutionHistory()) {
                if (record.isSuccess()
                        && !"router".equals(record.getNodeName())
                        && record.getOutputSummary() != null
                        && !record.getOutputSummary().isBlank()) {
                    expertOutputs.add(record.getOutputSummary());
                }
            }
        }
        if (expertOutputs.isEmpty()) {
            return "抱歉，暂时无法处理您的问题，请稍后再试或联系技术支持。";
        }
        if (expertOutputs.size() == 1) {
            return expertOutputs.get(0);
        }
        return String.join("\n\n---\n\n", expertOutputs);
    }

    private String detectIntentType(String userMessage) {
        if (userMessage == null) return "general_chat";
        String msg = userMessage.toLowerCase();
        if (containsAny(msg, "日志", "错误", "诊断", "检查", "排查", "故障", "log", "error", "diagnosis")) {
            return "system_diagnosis";
        } else if (containsAny(msg, "参数", "配置", "设置", "优化", "利用率", "套料", "spacing", "margin", "parameter")) {
            return "technical_question";
        } else if (containsAny(msg, "怎么", "如何", "什么是", "手册", "教程", "文档", "faq", "help", "帮助")) {
            return "knowledge_query";
        } else if (containsAny(msg, "报告", "导出", "备份", "执行", "生成", "report", "export", "backup")) {
            return "operation";
        }
        return "general_chat";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}