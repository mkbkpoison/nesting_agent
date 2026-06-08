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
    public String getName() { return "system_diagnosis"; }

    @Override
    public GraphState process(GraphState state) {
        log.info("SystemDiagnosisNode processing: {}", state.getUserMessage());
        String response = chatClient.prompt()
                .user(state.getUserMessage())
                .call()
                .content();
        state.addExecutionRecord(GraphState.NodeExecutionRecord.builder()
                .nodeName(getName()).role(AgentRole.SYSTEM_DIAGNOSIS)
                .inputSummary(state.getUserMessage()).outputSummary(response)
                .success(true).build());
        return state;
    }
}