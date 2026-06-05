package com.nesting.assistant.domain.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nesting.assistant.domain.entity.DiagnosticsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 诊断日志 Mapper
 */
@Mapper
public interface DiagnosticsLogMapper extends BaseMapper<DiagnosticsLog> {

    @Select("SELECT * FROM diagnostics_logs WHERE conversation_id = #{conversationId} ORDER BY created_at DESC")
    List<DiagnosticsLog> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") String conversationId);

    @Select("SELECT * FROM diagnostics_logs WHERE diagnosis_type = #{diagnosisType}")
    List<DiagnosticsLog> findByDiagnosisType(@Param("diagnosisType") String diagnosisType);

    @Select("SELECT * FROM diagnostics_logs WHERE status = #{status}")
    List<DiagnosticsLog> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM diagnostics_logs WHERE created_at > #{createdAt} ORDER BY created_at DESC")
    List<DiagnosticsLog> findByCreatedAtAfterOrderByCreatedAtDesc(@Param("createdAt") LocalDateTime createdAt);

    @Select("SELECT * FROM diagnostics_logs WHERE created_at BETWEEN #{start} AND #{end} ORDER BY created_at DESC")
    List<DiagnosticsLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
