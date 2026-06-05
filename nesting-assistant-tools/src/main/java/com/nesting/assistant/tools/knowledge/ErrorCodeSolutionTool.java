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
 * 错误代码解决方案检索工具
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorCodeSolutionTool {

    private final NestingRagService ragService;

    @Tool(description = "根据错误代码获取相关解决方案。" +
            "返回错误描述、原因分析和解决步骤。" +
            "当用户遇到特定错误代码时使用此工具快速获取解决方案。")
    public Map<String, Object> getRelatedSolutions(
            @ToolParam(description = "错误代码，如NEST-001, NEST-002等") String errorCode
    ) {
        log.info("Getting solutions for error code: {}", errorCode);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("errorCode", errorCode);

        List<Document> documents = ragService.searchErrorCode(errorCode);

        if (documents.isEmpty()) {
            result.put("found", false);
            result.put("message", "未找到错误代码 " + errorCode + " 的相关解决方案");
            result.put("suggestion", "请联系技术支持或查看系统日志获取更多信息");
            return result;
        }

        result.put("found", true);
        result.put("source", "knowledge_base");

        // 解析第一个匹配的文档
        Document doc = documents.get(0);
        String content = doc.getText();
        Map<String, Object> metadata = doc.getMetadata();

        result.put("module", metadata.get("module"));
        result.put("severity", metadata.get("severity"));
        result.put("fullContent", content);

        // 解析内容提取解决方案
        if (content.contains("解决方案:")) {
            String[] parts = content.split("解决方案:");
            if (parts.length > 1) {
                result.put("solution", parts[1].trim());
            }
        }
        if (content.contains("描述:")) {
            String[] parts = content.split("描述:");
            if (parts.length > 1) {
                String descPart = parts[1].split("\n")[0];
                result.put("description", descPart.trim());
            }
        }

        // 相关建议
        List<String> relatedActions = new ArrayList<>();
        relatedActions.add("查看系统日志获取详细错误信息");
        relatedActions.add("检查相关模块配置");
        relatedActions.add("如问题持续，联系技术支持");
        result.put("recommendedActions", relatedActions);

        return result;
    }
}
