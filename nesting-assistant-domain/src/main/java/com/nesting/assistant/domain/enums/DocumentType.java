package com.nesting.assistant.domain.enums;

import lombok.Getter;

/**
 * 知识文档类型枚举
 */
@Getter
public enum DocumentType {

    MANUAL("manual", "用户手册", "套料软件使用手册"),
    FAQ("faq", "常见问题", "FAQ问答对"),
    ERROR_CODE("error_code", "错误代码", "错误代码表及解决方案"),
    BEST_PRACTICE("best_practice", "最佳实践", "最佳实践指南"),
    CONFIG_GUIDE("config_guide", "配置指南", "配置参数说明"),
    TUTORIAL("tutorial", "教程", "操作教程");

    private final String code;
    private final String name;
    private final String description;

    DocumentType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
