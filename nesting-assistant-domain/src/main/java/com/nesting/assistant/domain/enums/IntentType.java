package com.nesting.assistant.domain.enums;

import lombok.Getter;

/**
 * 用户意图类型枚举
 */
@Getter
public enum IntentType {

    TECHNICAL_QUESTION("technical_question", "技术问题", "套料参数调优、配置问题、版本兼容性"),
    SYSTEM_DIAGNOSIS("system_diagnosis", "系统诊断", "日志检查、健康检查、资源监控"),
    KNOWLEDGE_QUERY("knowledge_query", "知识查询", "手册查询、FAQ、最佳实践"),
    OPERATION("operation", "操作执行", "执行命令、生成报告、备份配置"),
    GENERAL_CHAT("general_chat", "普通对话", "一般性问候或闲聊"),
    UNKNOWN("unknown", "未知意图", "无法识别的意图");

    private final String code;
    private final String name;
    private final String description;

    IntentType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static IntentType fromCode(String code) {
        for (IntentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
