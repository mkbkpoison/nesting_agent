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
        Matcher matcher = Pattern.compile(
                "Next\\s*[:：]\\s*(\\S+)", Pattern.CASE_INSENSITIVE
        ).matcher(response);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        if (response.toUpperCase().contains("END")) {
            return AgentGraph.END;
        }
        return AgentGraph.END;
    }

    private String parseFinalAnswer(String response) {
        if (response == null || response.isBlank()) return null;
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