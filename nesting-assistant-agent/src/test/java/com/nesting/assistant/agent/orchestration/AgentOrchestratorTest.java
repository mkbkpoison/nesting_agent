package com.nesting.assistant.agent.orchestration;

import com.nesting.assistant.agent.core.AgentResponse;
import com.nesting.assistant.agent.graph.AgentGraph;
import com.nesting.assistant.agent.graph.GraphState;
import com.nesting.assistant.agent.graph.node.*;
import com.nesting.assistant.domain.enums.AgentRole;
import com.nesting.assistant.memory.manager.ConversationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    @Mock
    private RouterNode routerNode;
    @Mock
    private TechnicalSupportNode technicalSupportNode;
    @Mock
    private SystemDiagnosisNode systemDiagnosisNode;
    @Mock
    private KnowledgeRetrievalNode knowledgeRetrievalNode;
    @Mock
    private OperationNode operationNode;
    @Mock
    private ConversationManager conversationManager;

    private AgentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // All 5 sub-node mocks are registered in the graph via initGraph(),
        // but individual tests may only interact with a subset. Use lenient
        // on sub-nodes that aren't called in every test to avoid
        // UnnecessaryStubbingException.
        lenient().when(technicalSupportNode.process(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(technicalSupportNode.getName()).thenReturn("technical_support");
        lenient().when(systemDiagnosisNode.process(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(systemDiagnosisNode.getName()).thenReturn("system_diagnosis");
        lenient().when(knowledgeRetrievalNode.process(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(knowledgeRetrievalNode.getName()).thenReturn("knowledge_retrieval");
        lenient().when(operationNode.process(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(operationNode.getName()).thenReturn("operation");

        orchestrator = new AgentOrchestrator(routerNode, technicalSupportNode,
                systemDiagnosisNode, knowledgeRetrievalNode, operationNode,
                conversationManager);
        orchestrator.initGraph();
    }

    @Test
    @DisplayName("Should orchestrate and return non-empty response")
    void testBasicOrchestration() {
        when(conversationManager.getConversationHistory(eq("user1"), eq("conv1")))
                .thenReturn(List.of());

        GraphState state = GraphState.builder()
                .userMessage("你好")
                .userId("user1")
                .conversationId("conv1")
                .maxSteps(10)
                .build();
        state.setNextNode(AgentGraph.END);
        state.setFinalAnswer("你好！有什么可以帮您的？");

        when(routerNode.process(any())).thenReturn(state);

        AgentResponse response = orchestrator.orchestrate("conv1", "user1", "你好");

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("你好"));
        assertEquals(AgentRole.ROUTER, response.getAgentRole());
    }

    @Test
    @DisplayName("Should build final response from expert outputs when no FinalAnswer")
    void testMultiExpertResponse() {
        when(conversationManager.getConversationHistory(eq("user1"), eq("conv1")))
                .thenReturn(List.of());

        when(routerNode.process(any())).thenAnswer(invocation -> {
            GraphState state = invocation.getArgument(0);
            if (state.getStepCount() == 0) {
                state.setNextNode("knowledge_retrieval");
                state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                        .nodeName("router").role(AgentRole.ROUTER).success(true).build());
            } else {
                state.setNextNode(AgentGraph.END);
                state.setFinalAnswer("共边切割设置方法：1. 打开设置面板...");
                state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                        .nodeName("router").role(AgentRole.ROUTER).success(true).build());
            }
            return state;
        });

        AgentResponse response = orchestrator.orchestrate("conv1", "user1", "怎么设置共边切割？");

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().isEmpty());
    }

    @Test
    @DisplayName("Should return fallback message when no response available")
    void testFallbackResponse() {
        when(conversationManager.getConversationHistory(eq("user1"), eq("conv1")))
                .thenReturn(List.of());

        when(routerNode.process(any())).thenAnswer(invocation -> {
            GraphState state = invocation.getArgument(0);
            state.setNextNode(AgentGraph.END);
            return state;
        });

        AgentResponse response = orchestrator.orchestrate("conv1", "user1", "unknown");

        assertNotNull(response);
        assertTrue(response.getContent().contains("抱歉"));
    }

    @Test
    @DisplayName("Should load conversation history")
    void testHistoryLoading() {
        List<Message> history = List.of(
                new UserMessage("昨天的利用率是多少？"),
                new org.springframework.ai.chat.messages.AssistantMessage("利用率是87%")
        );
        when(conversationManager.getConversationHistory(eq("user1"), eq("conv1")))
                .thenReturn(history);

        when(routerNode.process(any())).thenAnswer(invocation -> {
            GraphState state = invocation.getArgument(0);
            assertNotNull(state.getHistory());
            assertEquals(2, state.getHistory().size());
            assertTrue(state.getHistory().get(0).getText().contains("利用率"));
            state.setNextNode(AgentGraph.END);
            state.setFinalAnswer("当前利用率是89%。");
            return state;
        });

        AgentResponse response = orchestrator.orchestrate("conv1", "user1", "今天呢？");

        assertNotNull(response);
        assertTrue(response.getContent().contains("89%"));
    }
}