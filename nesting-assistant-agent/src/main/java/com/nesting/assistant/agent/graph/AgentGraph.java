package com.nesting.assistant.agent.graph;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 有向图定义。
 * 支持固定边（addEdge）和条件边（addConditionalEdge），
 * 由 GraphExecutor 在运行时使用 getNext() 驱动图遍历。
 */
public class AgentGraph {

    /** 终止节点标记 */
    public static final String END = "__END__";

    private final Map<String, GraphNode> nodes = new LinkedHashMap<>();
    private final Map<String, String> fixedEdges = new HashMap<>();
    private final Map<String, ConditionalRouter> conditionalEdges = new HashMap<>();
    private String entryPoint;

    // ==================== 图构造 API ====================

    public AgentGraph addNode(String name, GraphNode node) {
        nodes.put(name, node);
        return this;
    }

    /** 固定边：from 执行完毕后总是走到 to */
    public AgentGraph addEdge(String from, String to) {
        fixedEdges.put(from, to);
        return this;
    }

    /** 条件边：from 执行完毕后由 router 函数决定下一个节点 */
    public AgentGraph addConditionalEdge(String from, ConditionalRouter router) {
        conditionalEdges.put(from, router);
        return this;
    }

    public AgentGraph setEntryPoint(String name) {
        this.entryPoint = name;
        return this;
    }

    // ==================== 运行时查询 ====================

    public String getEntryPoint() {
        return entryPoint;
    }

    public GraphNode getNode(String name) {
        return nodes.get(name);
    }

    /** 根据当前节点和状态，确定下一个要执行的节点 */
    public String getNext(String currentNode, GraphState state) {
        ConditionalRouter router = conditionalEdges.get(currentNode);
        if (router != null) {
            return router.route(state);
        }
        return fixedEdges.getOrDefault(currentNode, END);
    }
}