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
    public String getName() { return "technical_support"; }

    @Override
    public GraphState process(GraphState state) {
        log.info("TechnicalSupportNode processing: {}", state.getUserMessage());
        String response = chatClient.prompt()
                .user(state.getUserMessage())
                .call()
                .content();
        state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                .nodeName(getName()).role(AgentRole.TECHNICAL_SUPPORT)
                .inputSummary(state.getUserMessage()).outputSummary(response)
                .success(true).build());
        return state;
    }
}