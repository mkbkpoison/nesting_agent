package com.nesting.assistant.domain.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nesting.assistant.domain.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT * FROM chat_messages WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") String conversationId);

    @Select("SELECT * FROM chat_messages WHERE conversation_id = #{conversationId} ORDER BY created_at DESC LIMIT 50")
    List<ChatMessage> findTop50ByConversationIdOrderByCreatedAtDesc(@Param("conversationId") String conversationId);

    @Select("SELECT COUNT(*) FROM chat_messages WHERE conversation_id = #{conversationId}")
    long countByConversationId(@Param("conversationId") String conversationId);

    @Delete("DELETE FROM chat_messages WHERE conversation_id = #{conversationId}")
    void deleteByConversationId(@Param("conversationId") String conversationId);
}
