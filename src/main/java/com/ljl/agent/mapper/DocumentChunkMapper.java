package com.ljl.agent.mapper;

import com.ljl.agent.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentChunkMapper {

    int insert(DocumentChunk chunk);

    DocumentChunk selectById(Long id);

    List<DocumentChunk> selectByDocumentId(
            Long documentId);

    DocumentChunk selectByDocumentIdAndChunkIndex(
            @Param("documentId") Long documentId,
            @Param("chunkIndex") Integer chunkIndex
    );
}