package com.nesting.assistant.agent.impl;

import com.nesting.assistant.agent.core.*;
import com.nesting.assistant.domain.enums.AgentRole;
import com.nesting.assistant.domain.enums.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 操作执行智能体
 * 负责执行系统检查命令、生成诊断报告、配置文件修复
 * 支持Tool Calling（调用ReportExportTool, ErrorLogTool, ConfigBackupTool等）
 */
@Slf4j
@Component
public class OperationAgent extends BaseAgent {

    private final ChatClient chatClient;

    private static final String OPERATION_PROMPT = """
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

    public OperationAgent(ChatClient.Builder chatClientBuilder,
                           ToolCallbackProvider fileOperationToolCallbackProvider) {
        super(AgentRole.OPERATION, OPERATION_PROMPT);
        ToolCallback[] tools = fileOperationToolCallbackProvider.getToolCallbacks();
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(tools)
                .defaultSystem(OPERATION_PROMPT)
                .build();
    }

    @Override
    public AgentResponse process(AgentContext context) {
        log.info("OperationAgent processing: {}", context.getUserMessage());

        String response = chatClient.prompt()
                .system(OPERATION_PROMPT)
                .user(context.getUserMessage())
                .call()
                .content();

        return AgentResponse.builder()
                .content(response)
                .agentRole(AgentRole.OPERATION)
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public boolean canHandle(AgentContext context) {
        if (context.getIntent() == null) {
            return false;
        }
        return context.getIntent().getIntentType() == IntentType.OPERATION;
    }
}
