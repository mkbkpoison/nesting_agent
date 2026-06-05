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
 * 相似案例检索工具
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarCaseTool {

    private final NestingRagService ragService;

    @Tool(description = "搜索与当前问题描述相似的历史案例和解决方案。" +
            "通过语义匹配找到相关问题及其解决方法。" +
            "当用户描述一个问题时使用此工具查找类似案例。")
    public List<Map<String, Object>> searchSimilarCases(
            @ToolParam(description = "问题描述或关键词") String description,
            @ToolParam(description = "返回结果数量，默认5", required = false) Integer topK
    ) {
        int maxResults = topK != null ? topK : 5;
        log.info("Searching similar cases: description={}", description);

        List<Document> documents = ragService.searchKnowledge(description, maxResults);

        List<Map<String, Object>> cases = new ArrayList<>();

        for (Document doc : documents) {
            Map<String, Object> caseEntry = new LinkedHashMap<>();
            caseEntry.put("content", doc.getText());
            caseEntry.put("type", doc.getMetadata().getOrDefault("type", "unknown"));
            caseEntry.put("category", doc.getMetadata().getOrDefault("category", "general"));
            caseEntry.put("tags", doc.getMetadata().getOrDefault("tags", ""));
            caseEntry.put("relevanceScore", doc.getMetadata().get("distance"));

            // 提取问题和回答（如果是FAQ格式）
            String text = doc.getText();
            if (text.contains("问题:") && text.contains("回答:")) {
                String[] parts = text.split("回答:");
                if (parts.length >= 1) {
                    String questionPart = parts[0].replace("问题:", "").trim();
                    caseEntry.put("question", questionPart);
                }
                if (parts.length >= 2) {
                    caseEntry.put("answer", parts[1].trim());
                }
            }

            cases.add(caseEntry);
        }

        return cases;
    }
}
