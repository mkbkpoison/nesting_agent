package com.nesting.assistant.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("conversations")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("user_id")
    private String userId;

    @TableField("title")
    private String title;

    @TableField("intent_type")
    private String intentType;

    @TableField("agent_role")
    private String agentRole;

    @TableField("message_count")
    private Integer messageCount;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("ended_at")
    private LocalDateTime endedAt;
}
