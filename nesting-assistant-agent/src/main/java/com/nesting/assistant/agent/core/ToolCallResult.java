package com.nesting.assistant.agent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallResult {

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 调用参数
     */
    private String arguments;

    /**
     * 返回结果
     */
    private Object result;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 执行时间(ms)
     */
    private long executionTimeMs;
}
