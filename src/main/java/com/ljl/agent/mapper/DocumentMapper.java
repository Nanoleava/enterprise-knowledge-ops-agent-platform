package com.ljl.agent.mapper;

import com.ljl.agent.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentMapper {

    int insert(Document document);

    Document selectById(Long id);

    List<Document> selectAll();

    List<Document> selectByKnowledgeBaseId(Long knowledgeBaseId);

    int deleteById(@Param("id") Long id);

    Document selectByKnowledgeBaseIdAndTitle(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("title") String title
    );

    List<Document> selectPage(
            @Param("keyword") String keyword,
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long countPage(
            @Param("keyword") String keyword,
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );
}