package com.nesting.assistant.rag.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 配置类
 * 自定义 TokenTextSplitter Bean，覆盖 Spring AI 默认值
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TokenTextSplitterConfig {

    private final RagProperties ragProperties;

    /**
     * 自定义 TokenTextSplitter
     * 优化参数适配中文文档和套料软件知识库场景：
     * - chunkSize=512：中文 token 密度高，512 比默认 800 更精确
     * - overlap=50：块间重叠保留上下文，避免句子在边界截断丢失语义
     * - keepSeparator=true：保留分隔符，确保句子完整
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        RagProperties.Chunk chunk = ragProperties.getChunk();

        log.info("Initializing TokenTextSplitter: chunkSize={}, minChunkChars={}, keepSeparator={}",
                chunk.getChunkSize(), chunk.getMinChunkSizeChars(),
                chunk.isKeepSeparator());

        return TokenTextSplitter.builder()
                .withChunkSize(chunk.getChunkSize())
                .withMinChunkSizeChars(chunk.getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(chunk.getMinChunkLengthToEmbed())
                .withMaxNumChunks(chunk.getMaxNumChunks())
                .withKeepSeparator(chunk.isKeepSeparator())
                .build();
    }

}
