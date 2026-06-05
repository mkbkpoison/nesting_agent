package com.nesting.assistant.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_messages")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("role")
    private String role;

    @TableField("content")
    private String content;

    @TableField("agent_role")
    private String agentRole;

    @TableField("tools_called")
    private String toolsCalled;

    @TableField("token_count")
    private Integer tokenCount;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public enum Role {
        USER, ASSISTANT, SYSTEM
    }
}
