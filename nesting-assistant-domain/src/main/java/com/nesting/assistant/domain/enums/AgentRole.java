package com.nesting.assistant.domain.enums;

import lombok.Getter;

/**
 * 智能体角色枚举
 */
@Getter
public enum AgentRole {

    ROUTER("router", "路由智能体", "意图识别与任务分发"),
    TECHNICAL_SUPPORT("tech_support", "技术支持智能体", "套料算法参数调优、配置检查、错误代码分析"),
    SYSTEM_DIAGNOSIS("system_diagnosis", "系统诊断智能体", "日志检查、数据库连接检测、资源监控、许可证检查"),
    KNOWLEDGE_RETRIEVAL("knowledge_retrieval", "知识检索智能体", "RAG知识库检索、相似问题匹配、解决方案推荐"),
    OPERATION("operation", "操作执行智能体", "执行系统检查命令、生成诊断报告、配置文件修复");

    private final String code;
    private final String name;
    private final String description;

    AgentRole(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static AgentRole fromCode(String code) {
        for (AgentRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
}
