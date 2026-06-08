package com.nesting.assistant.agent.graph;

import com.nesting.assistant.domain.enums.AgentRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图运行时状态，在节点间流动。
 * 每个节点读/写此状态，GraphExecutor 通过状态中的 nextNode 决定下一步走向。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphState {

    // ==================== 原始请求上下文 ====================

    /** 用户原始消息 */
    private String userMessage;

    /** 用户ID */
    private String userId;

    /** 会话ID */
    private String conversationId;

    // ==================== 运行时状态 ====================

    /** 当前正在执行的节点名称 */
    private String currentNode;

    /** 下一节点名称（由 RouterNode 设置，条件边读取） */
    private String nextNode;

    /** 已执行步数 */
    private int stepCount;

    /** 最大步数（防无限循环，默认10步） */
    private int maxSteps;

    /** 是否已终止 */
    private boolean terminated;

    /** 节点执行历史记录 */
    @Builder.Default
    private List<NodeExecutionRecord> executionHistory = new ArrayList<>();

    /** RouterNode 综合后的最终回答（当 Next: END 时可能附带） */
    private String finalAnswer;

    /** 节点间共享数据 */
    @Builder.Default
    private Map<String, Object> sharedData = new HashMap<>();

    // ==================== 内部类型 ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeExecutionRecord {
        private String nodeName;
        private AgentRole role;
        private String inputSummary;
        private String outputSummary;
        @Builder.Default
        private LocalDateTime timestamp = LocalDateTime.now();
        private boolean success;
        private String errorMessage;
    }

    // ==================== 辅助方法 ====================

    public void addExecutionRecord(NodeExecutionRecord record) {
        if (executionHistory == null) {
            executionHistory = new ArrayList<>();
        }
        executionHistory.add(record);
    }

    public void addSharedData(String key, Object value) {
        if (sharedData == null) {
            sharedData = new HashMap<>();
        }
        sharedData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSharedData(String key) {
        return sharedData != null ? (T) sharedData.get(key) : null;
    }

    /** 构建用户友好的人类可读历史摘要（供 RouterNode 提示词使用） */
    public String buildHistorySummary() {
        if (executionHistory == null || executionHistory.isEmpty()) {
            return "（暂无处理记录）";
        }
        StringBuilder sb = new StringBuilder();
        for (NodeExecutionRecord r : executionHistory) {
            if (r.isSuccess() && r.getOutputSummary() != null) {
                String trimmed = r.getOutputSummary().length() > 300
                        ? r.getOutputSummary().substring(0, 300) + "..."
                        : r.getOutputSummary();
                sb.append("- [").append(r.getNodeName()).append("]: ").append(trimmed).append("\n");
            } else if (!r.isSuccess()) {
                sb.append("- [").append(r.getNodeName()).append("]: ❌ 执行失败 - ")
                  .append(r.getErrorMessage()).append("\n");
            }
        }
        return sb.toString();
    }
}