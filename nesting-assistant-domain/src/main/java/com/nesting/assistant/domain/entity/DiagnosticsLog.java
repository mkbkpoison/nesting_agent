package com.nesting.assistant.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 诊断日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("diagnostics_logs")
public class DiagnosticsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("diagnosis_type")
    private String diagnosisType;

    @TableField("status")
    private String status;

    @TableField("summary")
    private String summary;

    @TableField("details")
    private String details;

    @TableField("recommendations")
    private String recommendations;

    @TableField("error_message")
    private String errorMessage;

    @TableField("execution_time_ms")
    private Long executionTimeMs;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
