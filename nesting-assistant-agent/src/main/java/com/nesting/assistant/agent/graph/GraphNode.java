package com.nesting.assistant.agent.graph;

/**
 * 图节点接口，所有图节点的基本抽象。
 * 每个节点接收当前 GraphState，处理后返回更新后的 GraphState。
 */
public interface GraphNode {

    /** 节点唯一标识，用于图中注册和边路由 */
    String getName();

    /**
     * 处理当前状态，返回更新后的状态。
     * 处理过程中可向 state.executionHistory 追加执行记录、
     * 读写 state.sharedData、设置 state.nextNode 等。
     */
    GraphState process(GraphState state);
}