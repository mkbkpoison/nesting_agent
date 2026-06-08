package com.nesting.assistant.agent.config;

import org.springframework.context.annotation.Configuration;

/**
 * ChatClient配置
 * 每个 Agent 在自己的构造函数中通过 ChatClient.Builder 创建 ChatClient，
 * 不再需要全局 ChatClient Bean。
 */
@Configuration
public class AgentConfig {
    // Agent 各自的 ChatClient 由各 Agent 自己构建
}
