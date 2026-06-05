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
import java.util.List;
import java.util.stream.Stream;

/**
 * 路由智能体
 * 负责意图识别（基于LLM）和任务分发
 * 支持Tool Calling
 */
@Slf4j
@Component
public class RouterAgent extends BaseAgent {

    private final ChatClient chatClient;
    private final TechnicalSupportAgent technicalSupportAgent;
    private final SystemDiagnosisAgent systemDiagnosisAgent;
    private final KnowledgeRetrievalAgent knowledgeRetrievalAgent;
    private final OperationAgent operationAgent;

    private static final String ROUTER_SYSTEM_PROMPT = """
            你是一个专业的套料软件智能助手路由器。你的任务是分析用户请求的意图，并决定如何最好地回答。

            用户意图类型包括：
            1. TECHNICAL_QUESTION - 技术问题：套料参数调优、配置检查、版本兼容性
            2. SYSTEM_DIAGNOSIS - 系统诊断：日志检查、健康检查、资源监控、许可证检查
            3. KNOWLEDGE_QUERY - 知识查询：手册查询、FAQ、最佳实践
            4. OPERATION - 操作执行：执行命令、生成报告、备份配置
            5. GENERAL_CHAT - 普通对话：一般性问候或闲聊

            请根据用户的消息判断意图，并提供专业、有帮助的回答。
            如果需要调用工具来获取信息，请直接调用相应的工具。
            """;

    private static final String INTENT_CLASSIFICATION_PROMPT = """
            请分析以下用户消息，判断其意图类型。
            只返回一个JSON对象，不要包含其他内容，格式如下：
            {"intentType": "TECHNICAL_QUESTION|SYSTEM_DIAGNOSIS|KNOWLEDGE_QUERY|OPERATION|GENERAL_CHAT", "confidence": 0.0-1.0, "reasoning": "简短判断理由"}

            意图定义：
            - TECHNICAL_QUESTION: 套料参数调优、配置检查、版本兼容性等纯技术问题
            - SYSTEM_DIAGNOSIS: 日志检查、健康检查、资源监控、许可证检查、故障排查
            - KNOWLEDGE_QUERY: 手册查询、FAQ、最佳实践、软件用法询问
            - OPERATION: 执行命令、生成报告、备份配置、导出数据
            - GENERAL_CHAT: 问候、闲聊、感谢等非技术性对话

            用户消息: "%s"
            """;

    public RouterAgent(ChatClient.Builder chatClientBuilder,
                       List<ToolCallbackProvider> toolCallbackProviders,
                       TechnicalSupportAgent technicalSupportAgent,
                       SystemDiagnosisAgent systemDiagnosisAgent,
                       KnowledgeRetrievalAgent knowledgeRetrievalAgent,
                       OperationAgent operationAgent) {
        super(AgentRole.ROUTER, ROUTER_SYSTEM_PROMPT);
        // 合并所有工具
        ToolCallback[] allTools = toolCallbackProviders.stream()
                .flatMap(p -> Arrays.stream(p.getToolCallbacks()))
                .toArray(ToolCallback[]::new);
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(allTools)
                .defaultSystem(ROUTER_SYSTEM_PROMPT)
                .build();
        this.technicalSupportAgent = technicalSupportAgent;
        this.systemDiagnosisAgent = systemDiagnosisAgent;
        this.knowledgeRetrievalAgent = knowledgeRetrievalAgent;
        this.operationAgent = operationAgent;
    }

    @Override
    public AgentResponse process(AgentContext context) {
        log.info("RouterAgent processing: {}", context.getUserMessage());

        // 使用LLM识别意图（不再是关键词匹配）
        AgentIntent intent = classifyIntentWithLLM(context.getUserMessage());
        context.setIntent(intent);
        log.info("Classified intent: {} (confidence: {}) — {}",
                intent.getIntentType(), intent.getConfidence(), intent.getReasoning());

        // 根据意图分发到对应的智能体
        AgentResponse response = dispatchToAgent(context, intent);

        response.setAgentRole(AgentRole.ROUTER);
        response.setIntentType(intent.getIntentType().getCode());

        return response;
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true; // Router可以处理所有请求
    }

    /**
     * 使用LLM分类用户意图（替代关键词匹配）
     */
    private AgentIntent classifyIntentWithLLM(String userMessage) {
        try {
            String prompt = String.format(INTENT_CLASSIFICATION_PROMPT, userMessage);

            String llmResponse = chatClient.prompt()
                    .system("你是一个精准的意图分类器。只返回JSON，不要包含markdown代码块标记。")
                    .user(prompt)
                    .call()
                    .content();

            log.debug("LLM intent classification response: {}", llmResponse);

            // 清理可能的 markdown 标记
            String cleaned = llmResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            // 提取JSON部分
            int jsonStart = cleaned.indexOf('{');
            int jsonEnd = cleaned.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
            }

            // 简单JSON解析
            String intentTypeStr = extractJsonValue(cleaned, "intentType");
            String confidenceStr = extractJsonValue(cleaned, "confidence");
            String reasoning = extractJsonValue(cleaned, "reasoning");

            IntentType intentType;
            try {
                intentType = IntentType.valueOf(intentTypeStr);
            } catch (Exception e) {
                intentType = IntentType.KNOWLEDGE_QUERY;
            }

            double confidence = 0.7;
            try {
                confidence = Double.parseDouble(confidenceStr);
            } catch (Exception e) {
                // 使用默认值
            }

            List<AgentRole> recommendedAgents = mapIntentToAgents(intentType);

            return AgentIntent.builder()
                    .intentType(intentType)
                    .confidence(confidence)
                    .reasoning(reasoning)
                    .recommendedAgents(recommendedAgents)
                    .needsTools(intentType != IntentType.GENERAL_CHAT)
                    .build();

        } catch (Exception e) {
            log.warn("LLM intent classification failed, falling back: {}", e.getMessage());
            return fallbackClassify(userMessage);
        }
    }

    /**
     * 兜底的关键词匹配分类（当LLM不可用时）
     */
    private AgentIntent fallbackClassify(String userMessage) {
        String message = userMessage.toLowerCase();

        IntentType intentType;
        double confidence;

        if (containsAny(message, "日志", "错误", "诊断", "检查", "排查", "故障", "log", "error", "diagnosis")) {
            intentType = IntentType.SYSTEM_DIAGNOSIS;
            confidence = 0.7;
        } else if (containsAny(message, "参数", "配置", "设置", "优化", "利用率", "套料", "spacing", "margin", "parameter")) {
            intentType = IntentType.TECHNICAL_QUESTION;
            confidence = 0.7;
        } else if (containsAny(message, "怎么", "如何", "什么是", "手册", "教程", "文档", "faq", "help", "帮助")) {
            intentType = IntentType.KNOWLEDGE_QUERY;
            confidence = 0.7;
        } else if (containsAny(message, "报告", "导出", "备份", "执行", "生成", "report", "export", "backup")) {
            intentType = IntentType.OPERATION;
            confidence = 0.7;
        } else if (containsAny(message, "你好", "您好", "hello", "hi", "谢谢", "感谢")) {
            intentType = IntentType.GENERAL_CHAT;
            confidence = 0.9;
        } else {
            intentType = IntentType.KNOWLEDGE_QUERY;
            confidence = 0.5;
        }

        return AgentIntent.builder()
                .intentType(intentType)
                .confidence(confidence)
                .reasoning("fallback keyword matching")
                .recommendedAgents(mapIntentToAgents(intentType))
                .needsTools(intentType != IntentType.GENERAL_CHAT)
                .build();
    }

    private List<AgentRole> mapIntentToAgents(IntentType intentType) {
        return switch (intentType) {
            case TECHNICAL_QUESTION -> List.of(AgentRole.TECHNICAL_SUPPORT);
            case SYSTEM_DIAGNOSIS -> List.of(AgentRole.SYSTEM_DIAGNOSIS, AgentRole.OPERATION);
            case KNOWLEDGE_QUERY -> List.of(AgentRole.KNOWLEDGE_RETRIEVAL);
            case OPERATION -> List.of(AgentRole.OPERATION);
            default -> List.of();
        };
    }

    /**
     * 分发到对应的智能体处理
     */
    private AgentResponse dispatchToAgent(AgentContext context, AgentIntent intent) {
        return switch (intent.getIntentType()) {
            case TECHNICAL_QUESTION -> technicalSupportAgent.process(context);
            case SYSTEM_DIAGNOSIS -> systemDiagnosisAgent.process(context);
            case KNOWLEDGE_QUERY -> knowledgeRetrievalAgent.process(context);
            case OPERATION -> operationAgent.process(context);
            case GENERAL_CHAT -> handleGeneralChat(context);
            default -> handleWithLLM(context);
        };
    }

    /**
     * 处理普通对话
     */
    private AgentResponse handleGeneralChat(AgentContext context) {
        String response = chatClient.prompt()
                .user(context.getUserMessage())
                .call()
                .content();

        return AgentResponse.of(response, AgentRole.ROUTER);
    }

    /**
     * 使用LLM处理
     */
    private AgentResponse handleWithLLM(AgentContext context) {
        String response = chatClient.prompt()
                .user(context.getUserMessage())
                .call()
                .content();

        return AgentResponse.of(response, AgentRole.ROUTER);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简单JSON字段值提取（无依赖）
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) return "";

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) return "";

        int start = colonIndex + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) {
            start++;
        }

        if (start >= json.length()) return "";

        if (json.charAt(start) == '"') {
            // 字符串值
            int end = start + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '\\') {
                    end += 2;
                    continue;
                }
                if (json.charAt(end) == '"') break;
                end++;
            }
            return json.substring(start + 1, end);
        } else {
            // 数字值
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end))
                    || json.charAt(end) == '.' || json.charAt(end) == '-')) {
                end++;
            }
            return json.substring(start, end);
        }
    }
}
