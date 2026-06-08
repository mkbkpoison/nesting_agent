package com.nesting.assistant.agent.tool;

import com.nesting.assistant.agent.core.AgentResponse;
import com.nesting.assistant.agent.impl.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Agent 委派工具
 * 将子 Agent 封装为 @Tool 方法，供 Router 的 LLM 通过 Tool Calling 调用。
 *
 * 这是多智能体编排的核心——Agent 之间通过工具调用协作，
 * LLM 自主决定何时调用哪个子 Agent、调几次、是否组合多个。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDelegationTools {

    private final TechnicalSupportAgent technicalSupportAgent;
    private final SystemDiagnosisAgent systemDiagnosisAgent;
    private final KnowledgeRetrievalAgent knowledgeRetrievalAgent;
    private final OperationAgent operationAgent;

    @Tool(description = """
            委托技术支持专家处理套料参数调优、配置检查、错误代码分析等技术问题。
            当你确定用户的问题属于技术咨询时调用此工具。
            参数: userMessage - 用户原始问题
            """)
    public String technicalSupport(
            @ToolParam(description = "用户原始问题") String userMessage
    ) {
        log.info("Delegating to TechnicalSupportAgent: {}", userMessage);
        AgentResponse response = technicalSupportAgent.process(
                technicalSupportAgent.buildContext(userMessage));
        return response.getContent();
    }

    @Tool(description = """
            委托系统诊断专家进行日志检查、数据库连接检测、资源监控、许可证检查等系统诊断。
            当用户报告系统故障、错误、性能问题时调用此工具。
            参数: userMessage - 用户原始问题
            """)
    public String systemDiagnosis(
            @ToolParam(description = "用户原始问题") String userMessage
    ) {
        log.info("Delegating to SystemDiagnosisAgent: {}", userMessage);
        AgentResponse response = systemDiagnosisAgent.process(
                systemDiagnosisAgent.buildContext(userMessage));
        return response.getContent();
    }

    @Tool(description = """
            委托知识助手从知识库中检索用户手册、FAQ、最佳实践、错误代码解决方案等信息。
            当用户询问软件用法、功能说明、操作指南时调用此工具。
            参数: userMessage - 用户原始问题
            """)
    public String knowledgeRetrieval(
            @ToolParam(description = "用户原始问题") String userMessage
    ) {
        log.info("Delegating to KnowledgeRetrievalAgent: {}", userMessage);
        AgentResponse response = knowledgeRetrievalAgent.process(
                knowledgeRetrievalAgent.buildContext(userMessage));
        return response.getContent();
    }

    @Tool(description = """
            委托操作执行助手执行系统操作任务，如导出诊断报告、备份配置、获取错误日志等。
            当用户需要执行具体操作或导出数据时调用此工具。
            参数: userMessage - 用户原始问题
            """)
    public String operation(
            @ToolParam(description = "用户原始问题") String userMessage
    ) {
        log.info("Delegating to OperationAgent: {}", userMessage);
        AgentResponse response = operationAgent.process(
                operationAgent.buildContext(userMessage));
        return response.getContent();
    }
}
