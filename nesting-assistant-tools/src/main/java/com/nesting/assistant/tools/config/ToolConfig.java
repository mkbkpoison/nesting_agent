package com.nesting.assistant.tools.config;

import com.nesting.assistant.tools.diagnosis.*;
import com.nesting.assistant.tools.file.*;
import com.nesting.assistant.tools.knowledge.*;
import com.nesting.assistant.tools.nesting.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {

    // ==================== 系统诊断工具 ====================

    @Bean
    public ToolCallbackProvider diagnosisToolCallbackProvider(
            SystemLogTool systemLogTool,
            DatabaseCheckTool databaseCheckTool,
            LicenseCheckTool licenseCheckTool,
            ResourceMonitorTool resourceMonitorTool,
            NestingEngineTool nestingEngineTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(systemLogTool, databaseCheckTool, licenseCheckTool,
                        resourceMonitorTool, nestingEngineTool)
                .build();
    }

    // ==================== 文件操作工具 ====================

    @Bean
    public ToolCallbackProvider fileOperationToolCallbackProvider(
            ReportExportTool reportExportTool,
            ErrorLogTool errorLogTool,
            ConfigBackupTool configBackupTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(reportExportTool, errorLogTool, configBackupTool)
                .build();
    }

    // ==================== 知识检索工具 ====================

    @Bean
    public ToolCallbackProvider knowledgeToolCallbackProvider(
            KnowledgeSearchTool knowledgeSearchTool,
            ErrorCodeSolutionTool errorCodeSolutionTool,
            SimilarCaseTool similarCaseTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(knowledgeSearchTool, errorCodeSolutionTool, similarCaseTool)
                .build();
    }

    // ==================== 套料专用工具 ====================

    @Bean
    public ToolCallbackProvider nestingToolCallbackProvider(
            ParameterCheckTool parameterCheckTool,
            ConfigOptimizerTool configOptimizerTool,
            UtilizationCalcTool utilizationCalcTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(parameterCheckTool, configOptimizerTool, utilizationCalcTool)
                .build();
    }
}
