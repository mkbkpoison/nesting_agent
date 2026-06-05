package com.nesting.assistant.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 配置属性类
 * 从 application.yml 的 nesting.assistant.rag 读取
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nesting.assistant.rag")
public class RagProperties {

    /** 默认检索返回条数 */
    private int topK = 10;

    /** 相似度阈值过滤 */
    private double similarityThreshold = 0.7;

    /** 分块配置 */
    private Chunk chunk = new Chunk();

    /** 各检索方法单独配置 */
    private Search search = new Search();

    @Getter
    @Setter
    public static class Chunk {
        /** 每个块的目标 token 数（中文场景建议 300-500） */
        private int chunkSize = 512;

        /** 最小块字符数（少于这个值与前一块合并） */
        private int minChunkSizeChars = 100;

        /** 最小分块长度（少于不单独embedding） */
        private int minChunkLengthToEmbed = 50;

        /** 最大块数限制 */
        private int maxNumChunks = 100;

        /** 是否保留分隔符 */
        private boolean keepSeparator = true;

        /** FAQ/错误码超过此字符数才分块（短文本不分块，保持语义完整） */
        private int faqSplitThreshold = 300;
    }

    @Getter
    @Setter
    public static class Search {
        /** 错误代码检索配置 */
        private MethodConfig errorCode = new MethodConfig(5, 0.5);

        /** FAQ 检索配置 */
        private MethodConfig faq = new MethodConfig(8, 0.75);

        /** 最佳实践检索配置 */
        private MethodConfig bestPractice = new MethodConfig(5, 0.7);

        /** 通用检索配置 */
        private MethodConfig general = new MethodConfig(10, 0.7);
    }

    @Getter
    @Setter
    public static class MethodConfig {
        private int topK;
        private double similarityThreshold;

        public MethodConfig() {
        }

        public MethodConfig(int topK, double similarityThreshold) {
            this.topK = topK;
            this.similarityThreshold = similarityThreshold;
        }
    }
}
