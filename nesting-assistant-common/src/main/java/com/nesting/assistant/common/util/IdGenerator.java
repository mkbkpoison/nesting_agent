package com.nesting.assistant.common.util;

import java.util.UUID;

/**
 * ID生成工具类
 */
public class IdGenerator {

    private IdGenerator() {}

    /**
     * 生成UUID（无横线）
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成会话ID
     */
    public static String conversationId() {
        return "conv_" + uuid();
    }

    /**
     * 生成请求ID
     */
    public static String requestId() {
        return "req_" + uuid();
    }

    /**
     * 生成文档ID
     */
    public static String documentId() {
        return "doc_" + uuid();
    }
}
