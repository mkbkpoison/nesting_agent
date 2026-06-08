# 图编排智能体架构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有的 Supervisor + Sub-agents（Tool Calling 通信）改造为混合式 StateGraph 编排

**Architecture:** 新建 `agent.graph` 包，包含 GraphNode / AgentGraph / GraphState / GraphExecutor 框架；RouterNode 保留 ReAct 推理能力但只输出路由决策，4 个子节点作为 GraphNode 注册，GraphExecutor 外层循环驱动调度

**Tech Stack:** Spring AI ChatClient, Spring Boot, Lombok

---

### Task 1: 创建 GraphNode 接口

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/GraphNode.java`

- [ ] **Step 1: Write GraphNode.java**

```java
package com.nesting.assistant.agent.graph;

/**
 * 图节点接口。
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
```

- [ ] **Step 2: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/GraphNode.java
git commit -m "feat(graph): add GraphNode interface

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2: 创建 GraphState

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/GraphState.java`

- [ ] **Step 1: Write GraphState.java**

```java
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
```

- [ ] **Step 2: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/GraphState.java
git commit -m "feat(graph): add GraphState with execution history tracking

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 3: 创建 ConditionalRouter 函数式接口

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/ConditionalRouter.java`

- [ ] **Step 1: Write ConditionalRouter.java**

```java
package com.nesting.assistant.agent.graph;

/**
 * 条件路由接口。
 * 根据当前状态返回下一个节点名称，返回 AgentGraph.END 则终止图执行。
 */
@FunctionalInterface
public interface ConditionalRouter {
    String route(GraphState state);
}
```

- [ ] **Step 2: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/ConditionalRouter.java
git commit -m "feat(graph): add ConditionalRouter functional interface

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 4: 创建 AgentGraph

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/AgentGraph.java`

- [ ] **Step 1: Write AgentGraph.java**

```java
package com.nesting.assistant.agent.graph;

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
        // 条件边优先
        ConditionalRouter router = conditionalEdges.get(currentNode);
        if (router != null) {
            return router.route(state);
        }
        // 固定边兜底
        return fixedEdges.getOrDefault(currentNode, END);
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/AgentGraph.java
git commit -m "feat(graph): add AgentGraph with fixed and conditional edges

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 5: 创建 GraphExecutor

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/GraphExecutor.java`

- [ ] **Step 1: Write GraphExecutor.java**

```java
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
```

- [ ] **Step 2: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/GraphExecutor.java
git commit -m "feat(graph): add GraphExecutor with loop and error handling

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 6: 创建 RouterNode

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/RouterNode.java`

- [ ] **Step 1: Write RouterNode.java**

```java
package com.nesting.assistant.agent.graph.node;

import com.nesting.assistant.agent.graph.AgentGraph;
import com.nesting.assistant.agent.graph.GraphNode;
import com.nesting.assistant.agent.graph.GraphState;
import com.nesting.assistant.domain.enums.AgentRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路由节点 — 混合式 ReAct 路由。
 *
 * 每次调用时用 LLM 分析当前 GraphState（用户问题 + 已执行历史），
 * 输出决策格式：
 *   Next: <专家名称>
 *   或
 *   Next: END
 *   Final Answer: <综合回答>
 *
 * 条件边读取 state.nextNode 进行分发。
 */
@Slf4j
@Component("graphRouterNode")
public class RouterNode implements GraphNode {

    private static final String SYSTEM_PROMPT = """
            你是一个智能助手编排路由器，负责分析用户问题并调度专家来处理。

            ## 可用专家

            1. technical_support - 技术支持专家
               处理：套料参数调优、配置检查、错误代码分析、版本兼容性
               调用条件：用户询问技术参数、配置、优化建议时

            2. system_diagnosis - 系统诊断专家
               处理：日志检查、数据库诊断、资源监控、许可证检查、故障排查
               调用条件：用户报告错误、系统异常、需要检查状态时

            3. knowledge_retrieval - 知识检索专家
               处理：用户手册查询、FAQ、最佳实践、错误代码查询
               调用条件：用户询问软件用法、功能说明、操作指南时

            4. operation - 操作执行专家
               处理：导出报告、备份配置、获取日志文件
               调用条件：用户需要执行具体操作或导出数据时

            ## 工作流程

            1. 分析用户问题，判断需要哪个专家处理
            2. 看到专家返回结果后，分析是否需要其他专家补充信息
            3. 所有信息充分时，直接给出完整的最终回答

            ## 输出格式

            需要调用专家时：
            Next: <专家名称>

            所有信息已足够，直接回答：
            Next: END
            Final Answer: <综合所有信息给出的完整回答>

            ## 规则

            - 对于简单问候（你好、谢谢、再见），直接 Next: END 即可
            - 一次只调用一个专家，等看到结果后再决定下一步
            - 最多调用 5 次专家，之后必须给出 Final Answer
            - 始终用中文回答
            """;

    private final ChatClient chatClient;
    private final int maxToolCalls;

    public RouterNode(ChatClient.Builder chatClientBuilder,
                      @Value("${nesting.assistant.max-tool-calls:5}") int maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public String getName() {
        return "router";
    }

    @Override
    public GraphState process(GraphState state) {
        log.info("RouterNode processing decision, step={}/{}", state.getStepCount() + 1, maxToolCalls);

        // 如果已超过最大专家调用次数，强制 END
        int expertCallCount = countExpertCalls(state);
        if (expertCallCount >= maxToolCalls) {
            log.warn("Reached max tool calls ({}), forcing END", maxToolCalls);
            state.setNextNode(AgentGraph.END);
            state.setFinalAnswer("已超出最大处理步骤，请简化您的问题或重新描述。");
            recordExecution(state, "Force END (maxToolCalls exceeded)");
            return state;
        }

        String userPrompt = String.format("""
                用户问题: %s

                已处理的专家结果:
                %s

                请分析当前情况，决定下一步。""",
                state.getUserMessage(),
                state.buildHistorySummary());

        String response = chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        log.debug("RouterNode raw response: {}", response);

        // 解析决策
        String decision = parseDecision(response);
        state.setNextNode(decision);

        // 尝试提取 Final Answer
        String finalAnswer = parseFinalAnswer(response);
        if (finalAnswer != null) {
            if (AgentGraph.END.equals(decision)) {
                state.setFinalAnswer(finalAnswer);
            } else {
                log.debug("Ignoring Final Answer from non-END decision: {}", finalAnswer);
            }
        }

        recordExecution(state, "Decision: " + decision);
        return state;
    }

    private String parseDecision(String response) {
        if (response == null || response.isBlank()) {
            return AgentGraph.END;
        }
        // 匹配 "Next: xxx" 或 "Next：xxx"
        Matcher matcher = Pattern.compile(
                "Next\\s*[:：]\\s*(\\S+)", Pattern.CASE_INSENSITIVE
        ).matcher(response);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        // 兜底：包含 END 关键字
        if (response.toUpperCase().contains("END")) {
            return AgentGraph.END;
        }
        return AgentGraph.END;
    }

    private String parseFinalAnswer(String response) {
        if (response == null || response.isBlank()) return null;
        // 匹配 "Final Answer:" 或 "最终回答：" 之后的内容
        Matcher matcher = Pattern.compile(
                "(?:Final Answer|最终回答)\\s*[:：]\\s*([\\s\\S]*)",
                Pattern.CASE_INSENSITIVE
        ).matcher(response);
        if (matcher.find()) {
            String answer = matcher.group(1).trim();
            return answer.isEmpty() ? null : answer;
        }
        return null;
    }

    private int countExpertCalls(GraphState state) {
        if (state.getExecutionHistory() == null) return 0;
        return (int) state.getExecutionHistory().stream()
                .filter(r -> r.isSuccess() && !"router".equals(r.getNodeName()))
                .count();
    }

    private void recordExecution(GraphState state, String summary) {
        state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                .nodeName(getName())
                .role(AgentRole.ROUTER)
                .inputSummary(state.getUserMessage())
                .outputSummary(summary)
                .success(true)
                .build());
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/RouterNode.java
git commit -m "feat(graph): add RouterNode with ReAct routing and Final Answer synthesis

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 7: 创建 TechnicalSupportNode

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/TechnicalSupportNode.java`

- [ ] **Step 1: Read existing TechnicalSupportAgent to reuse system prompt and tool config**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
cat nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/TechnicalSupportAgent.java
```

- [ ] **Step 2: Write TechnicalSupportNode.java**

```java
package com.nesting.assistant.agent.graph.node;

import com.nesting.assistant.agent.graph.GraphNode;
import com.nesting.assistant.agent.graph.GraphState;
import com.nesting.assistant.domain.enums.AgentRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * 技术支持节点。
 * 复用原有 TechnicalSupportAgent 的提示词和领域工具集。
 * 不自做路由，固定边回到 RouterNode。
 */
@Slf4j
@Component("graphTechnicalSupportNode")
public class TechnicalSupportNode implements GraphNode {

    private static final String SYSTEM_PROMPT = """
            你是套料软件的技术支持专家，精通板材套料优化算法、设备配置和故障诊断。

            你的专业领域包括：
            1. 套料参数优化：零件间距、板材边距、旋转角度、共边切割等
            2. 材料利用率提升：根据不同材料和零件特性提供优化建议
            3. 配置问题诊断：分析配置参数是否合理
            4. 错误代码解析：解释错误含义并提供解决方案
            5. 版本兼容性：解答软件版本相关问题

            回答问题时：
            - 提供具体、可操作的建议
            - 解释技术原理时使用通俗易懂的语言
            - 必要时调用相关工具获取系统信息
            - 对于复杂问题，提供分步骤的解决方案

            注意：你有多种工具可用，请根据用户问题选择合适的工具进行调用。
            """;

    private final ChatClient chatClient;

    public TechnicalSupportNode(ChatClient.Builder chatClientBuilder,
                                ToolCallbackProvider nestingToolCallbackProvider) {
        ToolCallback[] tools = nestingToolCallbackProvider.getToolCallbacks();
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(tools)
                .build();
    }

    @Override
    public String getName() {
        return "technical_support";
    }

    @Override
    public GraphState process(GraphState state) {
        log.info("TechnicalSupportNode processing: {}", state.getUserMessage());

        String response = chatClient.prompt()
                .user(state.getUserMessage())
                .call()
                .content();

        state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                .nodeName(getName())
                .role(AgentRole.TECHNICAL_SUPPORT)
                .inputSummary(state.getUserMessage())
                .outputSummary(response)
                .success(true)
                .build());

        return state;
    }
}
```

- [ ] **Step 3: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/TechnicalSupportNode.java
git commit -m "feat(graph): add TechnicalSupportNode with domain tools

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 8: 创建 SystemDiagnosisNode

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/SystemDiagnosisNode.java`

- [ ] **Step 1: Read existing SystemDiagnosisAgent to reuse prompt and tools**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
cat nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/SystemDiagnosisAgent.java
```

- [ ] **Step 2: Write SystemDiagnosisNode.java**

```java
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
@Component("graphSystemDiagnosisNode")
public class SystemDiagnosisNode implements GraphNode {

    private static final String SYSTEM_PROMPT = """
            你是套料软件的系统诊断专家，负责帮助用户排查和解决系统问题。

            你的诊断能力包括：
            1. 日志分析：检查系统日志，定位错误原因
            2. 数据库诊断：检测数据库连接状态和性能
            3. 资源监控：分析CPU、内存、磁盘使用情况
            4. 许可证检查：验证许可证状态和有效期
            5. 套料引擎状态：检查引擎运行状态和任务队列

            诊断流程：
            1. 首先调用相关诊断工具获取系统信息
            2. 分析诊断结果，找出问题根源
            3. 提供具体的解决建议
            4. 对于严重问题，建议用户联系技术支持

            可用的诊断工具：
            - checkSystemLogs: 检查系统日志
            - checkDatabaseConnection: 检查数据库连接
            - checkLicenseStatus: 检查许可证状态
            - getSystemResources: 获取系统资源使用情况
            - checkNestingEngineStatus: 检查套料引擎状态

            请根据用户的问题选择合适的工具进行调用。
            """;

    private final ChatClient chatClient;

    public SystemDiagnosisNode(ChatClient.Builder chatClientBuilder,
                               ToolCallbackProvider diagnosisToolCallbackProvider) {
        ToolCallback[] tools = diagnosisToolCallbackProvider.getToolCallbacks();
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(tools)
                .build();
    }

    @Override
    public String getName() {
        return "system_diagnosis";
    }

    @Override
    public GraphState process(GraphState state) {
        log.info("SystemDiagnosisNode processing: {}", state.getUserMessage());

        String response = chatClient.prompt()
                .user(state.getUserMessage())
                .call()
                .content();

        state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                .nodeName(getName())
                .role(AgentRole.SYSTEM_DIAGNOSIS)
                .inputSummary(state.getUserMessage())
                .outputSummary(response)
                .success(true)
                .build());

        return state;
    }
}
```

- [ ] **Step 3: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/SystemDiagnosisNode.java
git commit -m "feat(graph): add SystemDiagnosisNode with diagnosis tools

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 9: 创建 KnowledgeRetrievalNode

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/KnowledgeRetrievalNode.java`

- [ ] **Step 1: Read existing KnowledgeRetrievalAgent to reuse prompt and tools**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
cat nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/KnowledgeRetrievalAgent.java
```

- [ ] **Step 2: Write KnowledgeRetrievalNode.java**

```java
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
    public String getName() {
        return "knowledge_retrieval";
    }

    @Override
    public GraphState process(GraphState state) {
        log.info("KnowledgeRetrievalNode processing: {}", state.getUserMessage());

        String response = chatClient.prompt()
                .user(state.getUserMessage())
                .call()
                .content();

        state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                .nodeName(getName())
                .role(AgentRole.KNOWLEDGE_RETRIEVAL)
                .inputSummary(state.getUserMessage())
                .outputSummary(response)
                .success(true)
                .build());

        return state;
    }
}
```

- [ ] **Step 3: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/KnowledgeRetrievalNode.java
git commit -m "feat(graph): add KnowledgeRetrievalNode with RAG tools

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 10: 创建 OperationNode

**Files:**
- Create: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/OperationNode.java`

- [ ] **Step 1: Read existing OperationAgent to reuse prompt and tools**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
cat nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/OperationAgent.java
```

- [ ] **Step 2: Write OperationNode.java**

```java
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
@Component("graphOperationNode")
public class OperationNode implements GraphNode {

    private static final String SYSTEM_PROMPT = """
            你是套料软件的操作执行助手，帮助用户执行系统操作任务。

            你可以执行的操作包括：
            1. 诊断报告导出：生成PDF/HTML/JSON格式的系统诊断报告
            2. 配置备份：备份当前系统配置
            3. 错误日志查询：获取最近的错误日志记录

            可用的操作工具：
            - exportDiagnosticReport: 导出诊断报告
            - backupConfiguration: 备份配置
            - getRecentErrorLogs: 获取最近错误日志

            执行操作时：
            - 确认用户的需求和参数
            - 执行操作并报告结果
            - 提供下载链接或文件路径（如适用）
            - 对于危险操作，提醒用户注意事项

            安全原则：
            - 不执行可能影响系统稳定性的操作
            - 对于修改性操作，建议用户先备份

            请根据用户的需求选择合适的工具进行调用。
            """;

    private final ChatClient chatClient;

    public OperationNode(ChatClient.Builder chatClientBuilder,
                         ToolCallbackProvider fileOperationToolCallbackProvider) {
        ToolCallback[] tools = fileOperationToolCallbackProvider.getToolCallbacks();
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(tools)
                .build();
    }

    @Override
    public String getName() {
        return "operation";
    }

    @Override
    public GraphState process(GraphState state) {
        log.info("OperationNode processing: {}", state.getUserMessage());

        String response = chatClient.prompt()
                .user(state.getUserMessage())
                .call()
                .content();

        state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                .nodeName(getName())
                .role(AgentRole.OPERATION)
                .inputSummary(state.getUserMessage())
                .outputSummary(response)
                .success(true)
                .build());

        return state;
    }
}
```

- [ ] **Step 3: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/node/OperationNode.java
git commit -m "feat(graph): add OperationNode with file operation tools

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 11: 改造 AgentOrchestrator

**Files:**
- Modify: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/orchestration/AgentOrchestrator.java`

- [ ] **Step 1: Rewrite AgentOrchestrator.java**

```java
package com.nesting.assistant.agent.orchestration;

import com.nesting.assistant.agent.core.AgentResponse;
import com.nesting.assistant.agent.graph.*;
import com.nesting.assistant.agent.graph.node.*;
import com.nesting.assistant.domain.enums.AgentRole;
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

    private AgentGraph graph;
    private final GraphExecutor graphExecutor = new GraphExecutor();

    @PostConstruct
    public void initGraph() {
        graph = new AgentGraph()
                .setEntryPoint("router")
                // 注册节点
                .addNode("router", routerNode)
                .addNode("technical_support", technicalSupportNode)
                .addNode("system_diagnosis", systemDiagnosisNode)
                .addNode("knowledge_retrieval", knowledgeRetrievalNode)
                .addNode("operation", operationNode)
                // 条件边：router 根据 LLM 决策分发到子节点或 END
                .addConditionalEdge("router", state ->
                        state.getNextNode() != null ? state.getNextNode() : AgentGraph.END)
                // 固定边：所有子节点处理完后回到 router
                .addEdge("technical_support", "router")
                .addEdge("system_diagnosis", "router")
                .addEdge("knowledge_retrieval", "router")
                .addEdge("operation", "router");

        log.info("AgentGraph initialized with entryPoint=router, nodes={}", "5 nodes registered");
    }

    /**
     * 编排处理用户请求
     */
    public AgentResponse orchestrate(String conversationId, String userId, String userMessage) {
        log.info("Orchestrating request: userId={}, conversationId={}, message={}",
                userId, conversationId, userMessage);

        GraphState state = GraphState.builder()
                .conversationId(conversationId)
                .userId(userId != null ? userId : "default")
                .userMessage(userMessage)
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

        log.info("Orchestration complete: {} steps, agentRole={}",
                state.getStepCount(), response.getAgentRole());
        return response;
    }

    /**
     * 从 GraphState 构建最终返回给用户的文本。
     * 优先级：RouterNode 的 Final Answer > 单一子节点输出 > 多子节点拼接
     */
    private String buildFinalResponse(GraphState state) {
        // 优先使用 RouterNode 综合的 Final Answer
        if (state.getFinalAnswer() != null && !state.getFinalAnswer().isBlank()) {
            return state.getFinalAnswer();
        }

        // 收集所有成功执行的子节点输出
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

        // 多个专家输出以分隔线拼接
        return String.join("\n\n---\n\n", expertOutputs);
    }

    /**
     * 基于关键词的意图检测（仅用于响应标记，不做路由决策）
     */
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
```

- [ ] **Step 2: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/orchestration/AgentOrchestrator.java
git commit -m "refactor(orchestrator): use GraphExecutor with assembled AgentGraph

Replace direct RouterAgent call with graph-based orchestration.
RouterAgent + sub-agents + AgentDelegationTools are replaced
by GraphNode implementations in the graph package.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 12: 删除旧的 Agent 类和工具类

**Files:**
- Delete: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/RouterAgent.java`
- Delete: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/TechnicalSupportAgent.java`
- Delete: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/SystemDiagnosisAgent.java`
- Delete: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/KnowledgeRetrievalAgent.java`
- Delete: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/OperationAgent.java`
- Delete: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/tool/AgentDelegationTools.java`
- Delete: `nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/core/BaseAgent.java`

- [ ] **Step 1: Verify no remaining references to the old classes**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
grep -r "RouterAgent\|TechnicalSupportAgent\|SystemDiagnosisAgent\|KnowledgeRetrievalAgent\|OperationAgent\|AgentDelegationTools\|BaseAgent" \
  --include="*.java" \
  --exclude-dir=impl \
  --exclude-dir=tool \
  --exclude-dir=core \
  nesting-assistant-agent/src/
grep -r "RouterAgent\|TechnicalSupportAgent\|SystemDiagnosisAgent\|KnowledgeRetrievalAgent\|OperationAgent\|AgentDelegationTools\|BaseAgent" \
  --include="*.java" \
  nesting-assistant-api/src/
grep -r "RouterAgent\|TechnicalSupportAgent\|SystemDiagnosisAgent\|KnowledgeRetrievalAgent\|OperationAgent\|AgentDelegationTools\|BaseAgent" \
  --include="*.java" \
  nesting-assistant-starter/src/
```

预期：只有 `impl/`, `tool/`, `core/` 目录自身的引用，无外部引用。

- [ ] **Step 2: Delete old files**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
rm nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/RouterAgent.java
rm nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/TechnicalSupportAgent.java
rm nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/SystemDiagnosisAgent.java
rm nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/KnowledgeRetrievalAgent.java
rm nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl/OperationAgent.java
rm nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/tool/AgentDelegationTools.java
rm nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/core/BaseAgent.java
```

- [ ] **Step 3: 删除空目录（如果变空）**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
rmdir nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/impl 2>/dev/null || true
rmdir nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/tool 2>/dev/null || true
```

- [ ] **Step 4: Commit**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git add -A
git commit -m "refactor: remove old Agent classes, tools, and BaseAgent

Old RouterAgent + 4 sub-agent impl classes replaced by GraphNode
implementations in graph.node package.
AgentDelegationTools no longer needed (routing via graph edges).
BaseAgent replaced by GraphNode interface.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 13: 编译验证

- [ ] **Step 1: 编译项目检查无错误**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
./mvnw compile -pl nesting-assistant-agent -am -q 2>&1
```

预期输出：无错误，编译成功（BUILD SUCCESS）

如果编译失败，检查：
1. `@Component("graphXxxNode")` Bean 名称冲突（新节点和旧类同名？已被删除所以无冲突）
2. `GraphNode` 接口是否被正确 import
3. `ChatClient.Builder` 依赖是否注入正确

- [ ] **Step 2: 检查所有 GraphNode Bean 是否被 Spring 扫描到**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
grep -r "graph" nesting-assistant-agent/src/main/java/com/nesting/assistant/agent/graph/ --include="*.java" | grep "@Component" | head -20
```

预期输出：5 个 @Component Bean（RouterNode, TechnicalSupportNode, SystemDiagnosisNode, KnowledgeRetrievalNode, OperationNode）

- [ ] **Step 3: 最终提交**

```bash
cd /c/Users/Administrator/Desktop/nesting-assistant
git status
```