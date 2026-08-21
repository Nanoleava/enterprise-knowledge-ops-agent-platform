package com.ljl.agent.mapper;

import com.ljl.agent.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentMapper {

    int insert(Document document);

    int insertUploaded(Document document);

    Document selectById(Long id);

    List<Document> selectAll();

    List<Document> selectByKnowledgeBaseId(
            @Param("userId") Long userId,
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );

    int deleteByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    Document selectByKnowledgeBaseIdAndTitle(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("title") String title
    );

    List<Document> selectPage(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long countPage(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );

    int markParseProcessing(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("force") boolean force
    );

    int updateParseSuccess(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("content") String content
    );

    int updateParseFailed(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("processError") String processError
    );
}
