# 图编排智能体架构设计

## 概述

将当前 Supervisor + Sub-agents（通过 Tool Calling 通信）的智能体编排方式改造为**混合式 StateGraph 编排**。控制流从隐式的 LLM ReAct 内循环，变为显式的 GraphExecutor 外循环，同时保留 RouterNode 的多步 ReAct 推理能力。

## 当前架构

```
RouterAgent (LLM + ReAct + @Tool 子Agent)
  ├── @Tool technicalSupport    → TechnicalSupportAgent
  ├── @Tool systemDiagnosis     → SystemDiagnosisAgent
  ├── @Tool knowledgeRetrieval  → KnowledgeRetrievalAgent
  └── @Tool operation           → OperationAgent
```

问题：
- 控制流隐含在 LLM 的 ReAct 提示词中，不可观测、不可调试
- 子 Agent 作为 @Tool 注册，只能在 Router 的 ChatClient 上下文中运行
- 无法灵活扩展拓扑（如并行节点、条件分支）

## 目标架构（混合式 StateGraph）

```
                    ┌───────────────────┐
                    │   GraphExecutor   │ ← 外层循环驱动
                    └────────┬──────────┘
                             │
                    ┌────────▼──────────┐
                    │    RouterNode     │ ← LLM 多步 ReAct + 决策
                    │ (保留Thought过程)  │
                    └───┬────┬────┬─────┘
                        │    │    │
          ┌─────────────┘    │    └──────────────┐
          ▼                  ▼                   ▼
  TechnicalNode    DiagnosisNode    KnowledgeNode  ...
          │                  │                   │
          └──────────────────┼───────────────────┘
                             ▼
                    ┌───────────────────┐
                    │    RouterNode     │ ← 再次决策或 END
                    └───────────────────┘
```

### 关键变化

| 维度 | 当前 | 新架构 |
|------|------|--------|
| 控制流 | 隐式（LLM 内循环） | 显式（GraphExecutor 外循环） |
| 子 Agent | 作为 @Tool | 作为 GraphNode |
| 状态传递 | Tool 返回值 | GraphState（节点间流动） |
| 路由决策 | LLM 选工具 | RouterNode 输出专家名 → 条件边分发 |
| 循环机制 | maxToolCalls | maxSteps（GraphExecutor 控制） |
| 可观测性 | 无 | GraphState.nodeHistory 完整记录 |
| 拓扑扩展 | 扁平 | 可任意拓扑 |

## 类结构

### 框架层（新）

```
com.nesting.assistant.agent.graph
├── GraphNode.java           ── 节点接口
├── AgentGraph.java          ── 图定义（节点注册 + 边定义 + 入口）
├── GraphState.java          ── 运行时状态
├── GraphExecutor.java       ── 图执行器
└── node/
    ├── RouterNode.java      ── 路由节点（保留ReAct）
    ├── TechnicalSupportNode.java
    ├── SystemDiagnosisNode.java
    ├── KnowledgeRetrievalNode.java
    └── OperationNode.java
```

### GraphNode 接口

```java
public interface GraphNode {
    /** 节点唯一标识 */
    String getName();

    /**
     * 处理当前状态，返回更新后的状态。
     * 处理过程中可以向 state 追加消息、记录执行历史。
     */
    GraphState process(GraphState state);
}
```

### AgentGraph

```java
public class AgentGraph {
    public static final String END = "__END__";

    AgentGraph addNode(String name, GraphNode node);
    AgentGraph addEdge(String from, String to);                         // 固定边
    AgentGraph addConditionalEdge(String from, ConditionalRouter router); // 条件边
    AgentGraph setEntryPoint(String name);

    String getNext(String currentNode, GraphState state);
    GraphNode getNode(String name);
}
```

### ConditionalRouter 函数式接口

```java
@FunctionalInterface
public interface ConditionalRouter {
    /** 根据当前状态返回下一个节点名称，返回 END 则终止 */
    String route(GraphState state);
}
```

### GraphState

```java
public class GraphState {
    // 原始上下文
    private String userMessage;
    private String userId;
    private String conversationId;

    // 运行时状态
    private String currentNode;
    private String nextNode;
    private int stepCount;
    private int maxSteps = 10;
    private boolean terminated;
    private List<NodeExecutionRecord> executionHistory;
    private String finalAnswer;   // RouterNode 综合后的最终回答

    // 共享数据（跨节点传递）
    private Map<String, Object> sharedData;
}
```

### NodeExecutionRecord

```java
@Data
public class NodeExecutionRecord {
    private String nodeName;
    private AgentRole role;
    private String inputSummary;
    private String outputSummary;
    private LocalDateTime timestamp;
}
```

### GraphExecutor

```java
public class GraphExecutor {
    public GraphState execute(AgentGraph graph, GraphState initialState);
}
```

执行逻辑：
1. 从 `graph.getEntryPoint()` 获取起始节点
2. 循环：
   a. 获取当前节点 → `node.process(state)`
   b. 记录执行历史
   c. `graph.getNext(currentNode, state)` 获取下一节点
   d. 若返回 `END` 或 `stepCount >= maxSteps`，终止
3. 返回最终 GraphState

## 图拓扑定义

```
入口点: "router"

节点列表:
  router             (RouterNode)
  technical_support  (TechnicalSupportNode)
  system_diagnosis   (SystemDiagnosisNode)
  knowledge_retrieval(KnowledgeRetrievalNode)
  operation          (OperationNode)

条件边:
  router → ConditionalRouter(读取 RouterNode 输出的决策)
           可能的目标: technical_support | system_diagnosis |
                       knowledge_retrieval | operation | __END__

固定边:
  technical_support  → router
  system_diagnosis   → router
  knowledge_retrieval → router
  operation          → router
```

即：所有子节点处理完成后**必须回到 RouterNode**，由 RouterNode 决定下一步（继续调其他专家或 END）。

## RouterNode 的 ReAct 能力

RouterNode 的提示词设计：

```
你是一个智能助手编排路由器。

当前状态：
用户问题: {userMessage}
已处理的专家结果:
{executionHistory}

分析：{Thought 过程}

根据当前情况，决定下一步调用哪个专家：
- technical_support：技术支持（套料参数优化、配置检查）
- system_diagnosis：系统诊断（日志、数据库、许可证）
- knowledge_retrieval：知识检索（手册、FAQ、错误代码）
- operation：操作执行（导出报告、备份配置）

所有信息已足够时输出：END

最终输出格式（只输出一行，不要多余内容）：
Next: <专家名称或 END>
```

RouterNode 从 LLM 输出中解析 `Next:` 后面的值，写入 `state.nextNode`。条件边的 `ConditionalRouter` 直接读取 `state.nextNode`。

## 各 Node 职责

### RouterNode
- 使用 LLM 分析 GraphState（用户问题 + 已执行历史）
- 输出决策（下一个节点或 END）
- **保留 Thought 过程**：每次调用能看到完整的上下文

### TechnicalSupportNode
- 复用当前 TechnicalSupportAgent 的 ChatClient + 领域工具
- 处理完后将结果追加到 GraphState 的 executionHistory
- 不自做路由决策（固定边回 RouterNode）

### SystemDiagnosisNode
- 与上类似，复用当前系统诊断逻辑
- 处理完后回 RouterNode

### KnowledgeRetrievalNode
- 与上类似，复用当前 RAG 检索逻辑
- 处理完后回 RouterNode

### OperationNode
- 与上类似，复用当前操作执行逻辑
- 处理完后回 RouterNode

## 流程示例（用户问"报错 0xE001"）

```
初始 GraphState: userMessage="报错 0xE001，怎么回事？", history=[]

Step 1: GraphExecutor → RouterNode.process(state)
  [LLM Thought: 用户报告错误代码，需要先诊断系统状态]
  Next: system_diagnosis

Step 2: GraphExecutor 读条件边 → system_diagnosis
  SystemDiagnosisNode.process(state)
  [调用系统诊断工具 → 返回: 错误0xE001出现5次，均为License验证超时]
  state.executionHistory.add("system_diagnosis", result)
  固定边 → router

Step 3: RouterNode.process(state)
  [LLM Thought: 日志确认是License超时，查知识库找解决方案]
  Next: knowledge_retrieval

Step 4: GraphExecutor 读条件边 → knowledge_retrieval
  KnowledgeRetrievalNode.process(state)
  [RAG检索 → 返回: 0xE001 = License验证超时，建议重新激活许可证]
  state.executionHistory.add("knowledge_retrieval", result)
  固定边 → router

Step 5: RouterNode.process(state)
  [LLM Thought: 信息充分，综合回答]
  Next: END

Step 6: GraphExecutor → 终止，返回 state
```

## 边界条件

- **maxSteps = 10**：防止无限循环，超限直接终止，返回已有结果
- **问候语处理**：RouterNode 对问候直接输出 `Next: END`，零节点调用
- **节点异常**：某节点抛出异常 → 记录错误到 executionHistory → 回 RouterNode 决定是否重试或 END
- **空状态**：userMessage 为空 → RouterNode 直接 END，返回引导提示

## 文件变更清单

### 新建（9个文件）
1. `agent/graph/GraphNode.java`
2. `agent/graph/AgentGraph.java`
3. `agent/graph/GraphState.java`
4. `agent/graph/GraphExecutor.java`
5. `agent/graph/node/RouterNode.java`
6. `agent/graph/node/TechnicalSupportNode.java`
7. `agent/graph/node/SystemDiagnosisNode.java`
8. `agent/graph/node/KnowledgeRetrievalNode.java`
9. `agent/graph/node/OperationNode.java`

### 修改（3个文件）
1. `agent/orchestration/AgentOrchestrator.java` — 组装图 + 调用 GraphExecutor
2. `agent/core/BaseAgent.java` — 可选：精简或保留作为 GraphNode 的辅助基类
3. 可能需要小调整 `NestingAssistantService.java`

### 删除（5个文件）
1. `agent/impl/RouterAgent.java` — 被 RouterNode 取代
2. `agent/impl/TechnicalSupportAgent.java` — 被 TechnicalSupportNode 取代
3. `agent/impl/SystemDiagnosisAgent.java` — 被 SystemDiagnosisNode 取代
4. `agent/impl/KnowledgeRetrievalAgent.java` — 被 KnowledgeRetrievalNode 取代
5. `agent/impl/OperationAgent.java` — 被 OperationNode 取代
6. `agent/tool/AgentDelegationTools.java` — 不再需要（路由不再通过 @Tool）

## 后续扩展可能性

- **并行节点**：GraphExecutor 支持 Fan-out（多个子节点同时执行，合并结果再回 Router）
- **子图嵌套**：一个节点内部可以是另一个 AgentGraph
- **人机交互**：HumanInputNode 暂停图执行，等待用户输入后再继续
- **条件分支**：根据节点结果走不同的后续节点（如：错误严重 → 直接告警节点）