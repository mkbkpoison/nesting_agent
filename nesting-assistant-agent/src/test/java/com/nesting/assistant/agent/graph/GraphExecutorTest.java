package com.nesting.assistant.agent.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphExecutorTest {

    private GraphExecutor executor;
    private AgentGraph graph;

    @BeforeEach
    void setUp() {
        executor = new GraphExecutor();
        graph = new AgentGraph();
    }

    @Test
    @DisplayName("Should execute single node and reach END")
    void testSingleNodeExecution() {
        GraphNode echoNode = new GraphNode() {
            @Override
            public String getName() { return "echo"; }
            @Override
            public GraphState process(GraphState state) {
                state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                        .nodeName("echo").success(true).outputSummary("hello").build());
                return state;
            }
        };

        graph.addNode("echo", echoNode)
                .setEntryPoint("echo")
                .addEdge("echo", AgentGraph.END);

        GraphState state = GraphState.builder().userMessage("test").maxSteps(10).build();
        state = executor.execute(graph, state);

        assertTrue(state.isTerminated());
        assertEquals(1, state.getStepCount());
        assertEquals(1, state.getExecutionHistory().size());
    }

    @Test
    @DisplayName("Should stop when maxSteps reached")
    void testMaxStepsTermination() {
        GraphNode loopNode = new GraphNode() {
            @Override
            public String getName() { return "loop"; }
            @Override
            public GraphState process(GraphState state) {
                state.setNextNode("loop");
                return state;
            }
        };

        graph.addNode("loop", loopNode)
                .setEntryPoint("loop")
                .addConditionalEdge("loop", state -> state.getNextNode() != null ? state.getNextNode() : AgentGraph.END);

        GraphState state = GraphState.builder().userMessage("test").maxSteps(3).build();
        state = executor.execute(graph, state);

        assertTrue(state.isTerminated());
        assertEquals(3, state.getStepCount());
        assertTrue(state.getStepCount() <= state.getMaxSteps());
    }

    @Test
    @DisplayName("Should handle node execution failure")
    void testNodeFailure() {
        GraphNode failingNode = new GraphNode() {
            @Override
            public String getName() { return "fail"; }
            @Override
            public GraphState process(GraphState state) {
                throw new RuntimeException("Something went wrong");
            }
        };

        graph.addNode("fail", failingNode)
                .setEntryPoint("fail");

        GraphState state = GraphState.builder().userMessage("test").maxSteps(10).build();
        state = executor.execute(graph, state);

        assertTrue(state.isTerminated());
        // stepCount is not incremented when process() throws
        assertEquals(0, state.getStepCount());
        assertEquals(1, state.getExecutionHistory().size());
        assertFalse(state.getExecutionHistory().get(0).isSuccess());
        assertNotNull(state.getExecutionHistory().get(0).getErrorMessage());
    }

    @Test
    @DisplayName("Should follow conditional edge")
    void testConditionalEdge() {
        GraphNode router = new GraphNode() {
            @Override
            public String getName() { return "router"; }
            @Override
            public GraphState process(GraphState state) {
                if ("go_a".equals(state.getNextNode())) {
                    state.setNextNode("node_a");
                } else {
                    state.setNextNode("node_b");
                }
                state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                        .nodeName("router").success(true).build());
                return state;
            }
        };
        GraphNode nodeA = new GraphNode() {
            @Override
            public String getName() { return "node_a"; }
            @Override
            public GraphState process(GraphState state) {
                state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                        .nodeName("node_a").success(true).outputSummary("A done").build());
                return state;
            }
        };

        graph.addNode("router", router)
                .addNode("node_a", nodeA)
                .setEntryPoint("router")
                .addConditionalEdge("router", state -> state.getNextNode() != null ? state.getNextNode() : AgentGraph.END)
                .addEdge("node_a", AgentGraph.END);

        GraphState state = GraphState.builder().userMessage("test").maxSteps(10).build();
        state.setNextNode("go_a");
        state = executor.execute(graph, state);

        assertTrue(state.isTerminated());
        assertEquals(2, state.getStepCount());
        assertEquals(2, state.getExecutionHistory().size());
        assertEquals("node_a", state.getExecutionHistory().get(1).getNodeName());
    }

    @Test
    @DisplayName("Should return immediately if no entry point set")
    void testNoEntryPoint() {
        GraphState state = GraphState.builder().userMessage("test").maxSteps(10).build();
        state = executor.execute(graph, state);

        assertTrue(state.isTerminated());
        assertEquals(0, state.getStepCount());
    }
}