package com.nesting.assistant.agent.graph;

/**
 * 条件路由接口。
 * 根据当前状态返回下一个节点名称，返回 AgentGraph.END 则终止图执行。
 */
@FunctionalInterface
public interface ConditionalRouter {
    String route(GraphState state);
}