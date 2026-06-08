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
    public String getName() { return "operation"; }

    @Override
    public GraphState process(GraphState state) {
        log.info("OperationNode processing: {}", state.getUserMessage());

        StringBuilder responseBuilder = new StringBuilder();
        chatClient.prompt()
                .user(state.getUserMessage())
                .stream()
                .content()
                .doOnNext(token -> {
                    responseBuilder.append(token);
                    if (state.getOnToken() != null) {
                        state.getOnToken().accept(token);
                    }
                })
                .collectList()
                .block();
        String response = responseBuilder.toString();

        state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                .nodeName(getName()).role(AgentRole.OPERATION)
                .inputSummary(state.getUserMessage()).outputSummary(response)
                .success(true).build());
        return state;
    }
}