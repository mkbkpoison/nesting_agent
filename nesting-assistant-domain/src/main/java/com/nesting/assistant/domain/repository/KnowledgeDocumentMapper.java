package com.nesting.assistant.domain.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nesting.assistant.domain.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识文档 Mapper
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    @Select("SELECT * FROM knowledge_documents WHERE doc_id = #{docId}")
    KnowledgeDocument findByDocId(@Param("docId") String docId);

    @Select("SELECT * FROM knowledge_documents WHERE doc_code = #{docCode}")
    KnowledgeDocument findByDocCode(@Param("docCode") String docCode);

    @Select("SELECT * FROM knowledge_documents WHERE doc_type = #{docType}")
    List<KnowledgeDocument> findByDocType(@Param("docType") String docType);

    @Select("SELECT * FROM knowledge_documents WHERE doc_type = #{docType} AND active = true")
    List<KnowledgeDocument> findByDocTypeAndActiveTrue(@Param("docType") String docType);

    @Select("SELECT * FROM knowledge_documents WHERE category = #{category}")
    List<KnowledgeDocument> findByCategory(@Param("category") String category);

    @Select("SELECT * FROM knowledge_documents WHERE module = #{module}")
    List<KnowledgeDocument> findByModule(@Param("module") String module);

    @Select("SELECT * FROM knowledge_documents WHERE active = true")
    List<KnowledgeDocument> findByActiveTrue();

    @Select("SELECT COUNT(*) > 0 FROM knowledge_documents WHERE doc_code = #{docCode}")
    boolean existsByDocCode(@Param("docCode") String docCode);
}
