package com.nesting.assistant.rag.service;

import com.nesting.assistant.rag.config.RagProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 套料软件知识文档入库服务
 */
@Slf4j
@Service
public class NestingDocumentService {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private TokenTextSplitter textSplitter;

    @Resource
    private RagProperties ragProperties;

    /**
     * 入库文件
     */
    public void ingestFile(String filePath, Map<String, Object> metadata) {
        try {
            TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(filePath));
            List<Document> documents = reader.get();
            processDocuments(documents, metadata);
            log.info("Ingested file: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to ingest file: {}", filePath, e);
            throw new RuntimeException("Failed to ingest file: " + filePath, e);
        }
    }

    /**
     * 入库URL内容
     */
    public void ingestUrl(String url, Map<String, Object> metadata) {
        try {
            UrlResource resource = new UrlResource(new URI(url).toASCIIString());
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();
            processDocuments(documents, metadata);
            log.info("Ingested URL: {}", url);
        } catch (Exception e) {
            log.error("Failed to ingest URL: {}", url, e);
            throw new RuntimeException("Failed to ingest URL: " + url, e);
        }
    }

    /**
     * 入库文本内容
     */
    public void ingestText(String content, Map<String, Object> metadata) {
        Document doc = new Document(content, metadata);
        processDocuments(List.of(doc), metadata);
        log.debug("Ingested text content");
    }

    /**
     * 入库错误代码
     * 超过 chunk.faq-split-threshold 字符时自动分块
     */
    public void ingestErrorCode(String errorCode, String description, String solution, String module, String severity) {
        String content = String.format("""
                错误代码: %s
                模块: %s
                严重级别: %s
                描述: %s
                解决方案: %s
                """, errorCode, module, severity, description, solution);

        Map<String, Object> metadata = Map.of(
                "type", "error_code",
                "errorCode", errorCode,
                "module", module,
                "severity", severity,
                "ingestedAt", Instant.now().toString()
        );

        Document doc = new Document(content, metadata);
        // 长内容分块，短内容直接入库
        if (content.length() > ragProperties.getChunk().getFaqSplitThreshold()) {
            List<Document> chunks = textSplitter.apply(List.of(doc));
            vectorStore.add(chunks);
            log.info("Ingested error code (chunked into {}): {}", chunks.size(), errorCode);
        } else {
            vectorStore.add(List.of(doc));
            log.info("Ingested error code: {}", errorCode);
        }
    }

    /**
     * 入库FAQ
     * 超过 chunk.faq-split-threshold 字符时自动分块
     */
    public void ingestFAQ(String question, String answer, List<String> tags, String category) {
        String content = String.format("问题: %s\n回答: %s", question, answer);

        Map<String, Object> metadata = Map.of(
                "type", "faq",
                "question", question,
                "tags", String.join(",", tags),
                "category", category != null ? category : "general",
                "ingestedAt", Instant.now().toString()
        );

        Document doc = new Document(content, metadata);
        // 长内容分块，短内容直接入库
        if (content.length() > ragProperties.getChunk().getFaqSplitThreshold()) {
            List<Document> chunks = textSplitter.apply(List.of(doc));
            vectorStore.add(chunks);
            log.info("Ingested FAQ (chunked into {}): {}", chunks.size(),
                    question.substring(0, Math.min(50, question.length())));
        } else {
            vectorStore.add(List.of(doc));
            log.info("Ingested FAQ: {}", question.substring(0, Math.min(50, question.length())));
        }
    }

    /**
     * 入库最佳实践（始终分块，因为内容通常较长）
     */
    public void ingestBestPractice(String title, String content, String scenario, List<String> tags) {
        Map<String, Object> metadata = Map.of(
                "type", "best_practice",
                "title", title,
                "scenario", scenario != null ? scenario : "general",
                "tags", String.join(",", tags),
                "ingestedAt", Instant.now().toString()
        );

        Document doc = new Document(content, metadata);
        List<Document> chunks = textSplitter.apply(List.of(doc));
        vectorStore.add(chunks);
        log.info("Ingested best practice (chunked into {}): {}", chunks.size(), title);
    }

    /**
     * 删除文档（通过元数据过滤）
     */
    public void deleteByMetadata(String key, String value) {
        log.info("Deleting documents with {}={}", key, value);
        var builder = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();
        var filter = builder.eq(key, value).build();
        vectorStore.delete(filter);
    }

    private void processDocuments(List<Document> documents, Map<String, Object> metadata) {
        for (Document doc : documents) {
            metadata.forEach((k, v) -> doc.getMetadata().put(k, v != null ? v.toString() : ""));
            doc.getMetadata().putIfAbsent("ingestedAt", Instant.now().toString());
        }
        List<Document> chunks = textSplitter.apply(documents);
        vectorStore.add(chunks);
    }
}
