package com.nesting.assistant.rag.service;

import com.nesting.assistant.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 套料软件RAG检索服务
 * 所有检索参数从 RagProperties 读取，支持配置热加载
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NestingRagService {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    /**
     * 语义检索 — 使用配置的默认参数
     */
    public List<Document> searchKnowledge(String query) {
        return searchKnowledge(query, ragProperties.getTopK());
    }

    /**
     * 语义检索 — 自定义 topK
     */
    public List<Document> searchKnowledge(String query, int topK) {
        log.debug("Searching knowledge: query={}, topK={}", query, topK);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .build();
        return vectorStore.similaritySearch(request);
    }

    /**
     * 按文档类型过滤检索
     */
    public List<Document> searchByType(String query, String type, int topK) {
        log.debug("Searching knowledge by type: query={}, type={}, topK={}", query, type, topK);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .filterExpression(new FilterExpressionBuilder().eq("type", type).build())
                .build();
        return vectorStore.similaritySearch(request);
    }

    /**
     * 按模块过滤检索
     */
    public List<Document> searchByModule(String query, String module, int topK) {
        log.debug("Searching knowledge by module: query={}, module={}, topK={}", query, module, topK);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .filterExpression(new FilterExpressionBuilder().eq("module", module).build())
                .build();
        return vectorStore.similaritySearch(request);
    }

    /**
     * 错误代码检索 — 使用独立配置（topK=5, threshold=0.5）
     */
    public List<Document> searchErrorCode(String errorCode) {
        log.debug("Searching error code: {}", errorCode);
        RagProperties.MethodConfig cfg = ragProperties.getSearch().getErrorCode();

        SearchRequest request = SearchRequest.builder()
                .query("错误代码 " + errorCode)
                .topK(cfg.getTopK())
                .similarityThreshold(cfg.getSimilarityThreshold())
                .filterExpression(new FilterExpressionBuilder().eq("type", "error_code").build())
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * FAQ检索 — 使用独立配置（topK=8, threshold=0.75）
     */
    public List<Document> searchFAQ(String question, int topK) {
        log.debug("Searching FAQ: question={}", question);
        RagProperties.MethodConfig cfg = ragProperties.getSearch().getFaq();

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK > 0 ? topK : cfg.getTopK())
                .similarityThreshold(cfg.getSimilarityThreshold())
                .filterExpression(new FilterExpressionBuilder().eq("type", "faq").build())
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 最佳实践检索 — 使用独立配置（topK=5, threshold=0.7）
     */
    public List<Document> searchBestPractices(String scenario, int topK) {
        log.debug("Searching best practices: scenario={}", scenario);
        RagProperties.MethodConfig cfg = ragProperties.getSearch().getBestPractice();

        SearchRequest request = SearchRequest.builder()
                .query(scenario)
                .topK(topK > 0 ? topK : cfg.getTopK())
                .similarityThreshold(cfg.getSimilarityThreshold())
                .filterExpression(new FilterExpressionBuilder().eq("type", "best_practice").build())
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 复合条件检索
     */
    public List<Document> searchWithFilters(String query, String type, String module, int topK) {
        log.debug("Searching with filters: query={}, type={}, module={}", query, type, module);
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var typeFilter = builder.eq("type", type);
        var finalFilter = (module != null && !module.isEmpty())
                ? builder.and(typeFilter, builder.eq("module", module)).build()
                : typeFilter.build();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK > 0 ? topK : ragProperties.getTopK())
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .filterExpression(finalFilter)
                .build();

        return vectorStore.similaritySearch(request);
    }
}
