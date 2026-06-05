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
 * 技术支持智能体
 * 负责套料参数调优、配置检查、错误代码分析
 * 支持Tool Calling（调用ParameterCheckTool, ConfigOptimizerTool, UtilizationCalcTool等）
 */
@Slf4j
@Component
public class TechnicalSupportAgent extends BaseAgent {

    private final ChatClient chatClient;

    private static final String TECH_SUPPORT_PROMPT = """
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
            - 必要时调用相关工具获取系统信息（如 checkNestingParameters、optimizeNestingConfig、calculateMaterialUtilization 等）
            - 对于复杂问题，提供分步骤的解决方案

            注意：你有多种工具可用，请根据用户问题选择合适的工具进行调用。
            """;

    public TechnicalSupportAgent(ChatClient.Builder chatClientBuilder,
                                  ToolCallbackProvider nestingToolCallbackProvider) {
        super(AgentRole.TECHNICAL_SUPPORT, TECH_SUPPORT_PROMPT);
        ToolCallback[] tools = nestingToolCallbackProvider.getToolCallbacks();
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(tools)
                .defaultSystem(TECH_SUPPORT_PROMPT)
                .build();
    }

    @Override
    public AgentResponse process(AgentContext context) {
        log.info("TechnicalSupportAgent processing: {}", context.getUserMessage());

        String response = chatClient.prompt()
                .system(TECH_SUPPORT_PROMPT)
                .user(context.getUserMessage())
                .call()
                .content();

        return AgentResponse.builder()
                .content(response)
                .agentRole(AgentRole.TECHNICAL_SUPPORT)
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public boolean canHandle(AgentContext context) {
        if (context.getIntent() == null) {
            return false;
        }
        return context.getIntent().getIntentType() == IntentType.TECHNICAL_QUESTION;
    }
}
