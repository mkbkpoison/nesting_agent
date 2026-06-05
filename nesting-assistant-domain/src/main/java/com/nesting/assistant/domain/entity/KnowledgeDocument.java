package com.nesting.assistant.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识文档实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_documents")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("doc_id")
    private String docId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("doc_type")
    private String docType;

    @TableField("doc_code")
    private String docCode;

    @TableField("category")
    private String category;

    @TableField("tags")
    private String tags;

    @TableField("module")
    private String module;

    @TableField("severity")
    private String severity;

    @TableField("solution")
    private String solution;

    @TableField("source")
    private String source;

    @TableField("version")
    private String version;

    @TableField("metadata")
    private String metadata;

    @TableField("vector_id")
    private String vectorId;

    @TableField("active")
    private Boolean active;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
