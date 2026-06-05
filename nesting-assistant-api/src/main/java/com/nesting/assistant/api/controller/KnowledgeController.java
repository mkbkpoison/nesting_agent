package com.nesting.assistant.api.controller;

import com.nesting.assistant.common.model.ApiResponse;
import com.nesting.assistant.rag.service.NestingDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库接口", description = "知识库文档管理API")
public class KnowledgeController {

    private final NestingDocumentService documentService;

    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传知识库文档")
    public ApiResponse<Void> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(value = "version", required = false) String version) {
        log.info("Uploading document: {}, type: {}", file.getOriginalFilename(), type);

        try {
            String filePath = saveFile(file);
            documentService.ingestFile(filePath, Map.of(
                    "type", type,
                    "version", version != null ? version : "1.0",
                    "filename", file.getOriginalFilename()
            ));
            return ApiResponse.success("文档上传成功", null);
        } catch (Exception e) {
            log.error("Failed to upload document", e);
            return ApiResponse.error("文档上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/error-code")
    @Operation(summary = "添加错误代码", description = "添加错误代码及解决方案")
    public ApiResponse<Void> addErrorCode(
            @RequestParam String code,
            @RequestParam String description,
            @RequestParam String solution,
            @RequestParam String module,
            @RequestParam(defaultValue = "error") String severity) {
        log.info("Adding error code: {}", code);

        documentService.ingestErrorCode(code, description, solution, module, severity);
        return ApiResponse.success("错误代码添加成功", null);
    }

    @PostMapping("/faq")
    @Operation(summary = "添加FAQ", description = "添加FAQ问答对")
    public ApiResponse<Void> addFAQ(
            @RequestParam String question,
            @RequestParam String answer,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String category) {
        log.info("Adding FAQ: {}", question.substring(0, Math.min(50, question.length())));

        List<String> tagList = tags != null ? List.of(tags.split(",")) : List.of();
        documentService.ingestFAQ(question, answer, tagList, category);

        return ApiResponse.success("FAQ添加成功", null);
    }

    @PostMapping("/best-practice")
    @Operation(summary = "添加最佳实践", description = "添加最佳实践文档")
    public ApiResponse<Void> addBestPractice(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String scenario,
            @RequestParam(required = false) String tags) {
        log.info("Adding best practice: {}", title);

        List<String> tagList = tags != null ? List.of(tags.split(",")) : List.of();
        documentService.ingestBestPractice(title, content, scenario, tagList);

        return ApiResponse.success("最佳实践添加成功", null);
    }

    private String saveFile(MultipartFile file) throws Exception {
        // 简化实现，实际应该保存到文件存储
        String uploadDir = System.getProperty("java.io.tmpdir") + "/nesting-uploads/";
        java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir);
        java.nio.file.Files.createDirectories(dir);

        java.nio.file.Path filePath = dir.resolve(file.getOriginalFilename());
        file.transferTo(filePath.toFile());

        return filePath.toString();
    }
}
