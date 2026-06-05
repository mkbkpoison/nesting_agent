package com.nesting.assistant.domain.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nesting.assistant.domain.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 对话 Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversations WHERE conversation_id = #{conversationId}")
    Conversation findByConversationId(@Param("conversationId") String conversationId);

    @Select("SELECT * FROM conversations WHERE conversation_id = #{conversationId} AND user_id = #{userId}")
    Conversation findByConversationIdAndUserId(@Param("conversationId") String conversationId, @Param("userId") String userId);

    @Select("SELECT * FROM conversations WHERE user_id = #{userId} ORDER BY updated_at DESC")
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(@Param("userId") String userId);

    @Select("SELECT * FROM conversations WHERE user_id = #{userId} AND status = #{status} ORDER BY updated_at DESC")
    List<Conversation> findByUserIdAndStatusOrderByUpdatedAtDesc(@Param("userId") String userId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM conversations WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") String userId);

    @Update("DELETE FROM conversations WHERE conversation_id = #{conversationId}")
    void deleteByConversationId(@Param("conversationId") String conversationId);
}
