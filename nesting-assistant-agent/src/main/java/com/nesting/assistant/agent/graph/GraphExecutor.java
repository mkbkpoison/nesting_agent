package com.nesting.assistant.agent.graph;

import lombok.extern.slf4j.Slf4j;

/**
 * 图执行器。
 * 从入口节点开始，循环驱动图遍历：
 * 1. 取当前节点 → node.process(state)
 * 2. graph.getNext(currentNode, state) 决定下一步
 * 3. 遇到 END 或 stepCount >= maxSteps 时终止
 */
@Slf4j
public class GraphExecutor {

    /**
     * 执行图，返回最终状态。
     * @param graph 已装配好的图定义
     * @param initialState 初始状态（至少含 userMessage）
     * @return 执行完毕的最终状态
     */
    public GraphState execute(AgentGraph graph, GraphState initialState) {
        String nodeName = graph.getEntryPoint();
        if (nodeName == null) {
            log.error("Graph has no entry point set");
            initialState.setTerminated(true);
            return initialState;
        }

        GraphState state = initialState;
        state.setStepCount(0);
        state.setTerminated(false);

        while (!AgentGraph.END.equals(nodeName) && state.getStepCount() < state.getMaxSteps()) {
            GraphNode node = graph.getNode(nodeName);
            if (node == null) {
                log.error("Node not found in graph: '{}'", nodeName);
                state.setTerminated(true);
                break;
            }

            state.setCurrentNode(nodeName);
            log.info("Graph step {}: executing node [{}]", state.getStepCount() + 1, nodeName);

            try {
                state = node.process(state);
            } catch (Exception e) {
                log.error("Node [{}] execution failed: {}", nodeName, e.getMessage(), e);
                state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                        .nodeName(nodeName)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
                state.setTerminated(true);
                break;
            }

            state.setStepCount(state.getStepCount() + 1);
            nodeName = graph.getNext(nodeName, state);
        }

        if (state.getStepCount() >= state.getMaxSteps()) {
            log.warn("Graph execution terminated by maxSteps ({}) without reaching END", state.getMaxSteps());
        }

        state.setTerminated(true);
        log.info("Graph execution completed in {} steps", state.getStepCount());
        return state;
    }
}