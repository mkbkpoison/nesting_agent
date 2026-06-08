package com.nesting.assistant.agent.impl;

import com.nesting.assistant.agent.core.*;
import com.nesting.assistant.agent.tool.AgentDelegationTools;
import com.nesting.assistant.domain.enums.AgentRole;
import com.nesting.assistant.domain.enums.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 路由智能体 — 监督者模式 + 显式 ReAct
 *
 * 使用 ReAct（Reasoning + Acting）范式工作：
 * Thought  → 分析用户需求，决定下一步
 * Action   → 调用子 Agent 工具
 * Observation → 工具返回结果
 * Final Answer → 综合所有信息回答用户
 *
 * 最大工具调用步数由配置 nesting.assistant.max-tool-calls 控制
 */
@Slf4j
@Component
public class RouterAgent extends BaseAgent {

    private final ChatClient chatClient;
    private final int maxToolCalls;

    public RouterAgent(ChatClient.Builder chatClientBuilder,
                       AgentDelegationTools delegationTools,
                       @Value("${nesting.assistant.max-tool-calls:5}") int maxToolCalls) {
        super(AgentRole.ROUTER, buildReActPrompt(maxToolCalls));
        this.maxToolCalls = maxToolCalls;
        this.chatClient = chatClientBuilder
                .defaultTools(delegationTools)
                .defaultSystem(buildReActPrompt(maxToolCalls))
                .build();
    }

    private static String buildReActPrompt(int maxToolCalls) {
        return String.format("""
                你是一个专业的套料软件智能助手，使用 ReAct 模式工作。

                ## 可用专家工具

                你有以下专家可供调用：

                1. technicalSupport - 技术支持专家
                   处理：套料参数调优、配置检查、错误代码分析、版本兼容性
                   调用条件：用户询问技术参数、配置、优化建议时

                2. systemDiagnosis - 系统诊断专家
                   处理：日志检查、数据库诊断、资源监控、许可证检查、故障排查
                   调用条件：用户报告错误、系统异常、需要检查状态时

                3. knowledgeRetrieval - 知识检索专家
                   处理：用户手册查询、FAQ、最佳实践、错误代码查询
                   调用条件：用户询问软件用法、功能说明、操作指南时

                4. operation - 操作执行专家
                   处理：导出报告、备份配置、获取日志文件
                   调用条件：用户需要执行具体操作或导出数据时

                ## ReAct 工作流程

                请严格按以下格式思考和输出：

                Thought: 分析用户需求，判断需要做什么。
                Action: 工具名称（从上面选择）
                Action Input: 传给工具的用户问题

                当工具返回结果后，继续分析：

                Thought: 分析工具返回的结果，决定下一步。
                Action: （可选）再次调用其他工具
                Action Input: （可选）

                当信息收集完毕，输出：

                Final Answer: 综合所有工具返回的信息，用中文给出完整、友好的回答

                ## 限制

                - 最多调用 %d 次工具，之后必须给出 Final Answer
                - 对于简单问候（你好、谢谢、再见），直接输出 Final Answer，不要调用工具
                - 如果一次工具调用已经获得足够信息，不要重复调用
                - 始终用中文思考和回答
                """, maxToolCalls);
    }

    @Override
    public AgentResponse process(AgentContext context) {
        log.info("RouterAgent processing: {}, maxToolCalls={}", context.getUserMessage(), maxToolCalls);

        String response = chatClient.prompt()
                .user(context.getUserMessage())
                .call()
                .content();

        return AgentResponse.builder()
                .content(response)
                .agentRole(AgentRole.ROUTER)
                .intentType(detectIntentType(context.getUserMessage()))
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    private String detectIntentType(String userMessage) {
        String msg = userMessage.toLowerCase();
        if (containsAny(msg, "日志", "错误", "诊断", "检查", "排查", "故障", "log", "error", "diagnosis")) {
            return IntentType.SYSTEM_DIAGNOSIS.getCode();
        } else if (containsAny(msg, "参数", "配置", "设置", "优化", "利用率", "套料", "spacing", "margin", "parameter")) {
            return IntentType.TECHNICAL_QUESTION.getCode();
        } else if (containsAny(msg, "怎么", "如何", "什么是", "手册", "教程", "文档", "faq", "help", "帮助")) {
            return IntentType.KNOWLEDGE_QUERY.getCode();
        } else if (containsAny(msg, "报告", "导出", "备份", "执行", "生成", "report", "export", "backup")) {
            return IntentType.OPERATION.getCode();
        }
        return IntentType.KNOWLEDGE_QUERY.getCode();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
