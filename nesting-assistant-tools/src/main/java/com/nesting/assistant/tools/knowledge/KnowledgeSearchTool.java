package com.nesting.assistant.tools.knowledge;

import com.nesting.assistant.rag.service.NestingRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 知识库搜索工具
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSearchTool {

    private final NestingRagService ragService;

    @Tool(description = "搜索套料软件知识库，包括用户手册、FAQ、最佳实践等文档。" +
            "返回与查询最相关的知识文档片段。" +
            "当用户询问软件使用方法、功能说明等问题时使用此工具。")
    public List<Map<String, Object>> searchKnowledgeBase(
            @ToolParam(description = "搜索查询语句") String query,
            @ToolParam(description = "返回结果数量，默认5", required = false) Integer topK
    ) {
        int maxResults = topK != null ? topK : 5;
        log.info("Searching knowledge base: query={}, topK={}", query, maxResults);

        List<Document> documents = ragService.searchKnowledge(query, maxResults);

        List<Map<String, Object>> results = new ArrayList<>();
        for (Document doc : documents) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("content", doc.getText());
            entry.put("metadata", doc.getMetadata());
            entry.put("relevanceScore", doc.getMetadata().get("distance"));
            results.add(entry);
        }

        return results;
    }
}
