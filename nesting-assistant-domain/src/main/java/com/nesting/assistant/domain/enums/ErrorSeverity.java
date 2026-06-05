package com.nesting.assistant.domain.enums;

import lombok.Getter;

/**
 * 错误级别枚举
 */
@Getter
public enum ErrorSeverity {

    CRITICAL("critical", "严重", "系统无法继续运行，需要立即处理"),
    ERROR("error", "错误", "功能受影响，但系统可继续运行"),
    WARNING("warning", "警告", "潜在问题，建议关注"),
    INFO("info", "信息", "一般性提示信息"),
    DEBUG("debug", "调试", "调试详细信息");

    private final String code;
    private final String name;
    private final String description;

    ErrorSeverity(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
