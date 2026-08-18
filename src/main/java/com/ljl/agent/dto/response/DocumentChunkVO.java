package com.ljl.agent.dto.response;

import com.ljl.agent.entity.DocumentChunk;

import java.time.LocalDateTime;

public class DocumentChunkVO {

    private Long id;

    private Long documentId;

    private Long knowledgeBaseId;

    private Integer chunkIndex;

    private String content;

    private String metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static DocumentChunkVO from(
            DocumentChunk chunk) {

        DocumentChunkVO vo =
                new DocumentChunkVO();

        vo.setId(chunk.getId());
        vo.setDocumentId(chunk.getDocumentId());
        vo.setKnowledgeBaseId(
                chunk.getKnowledgeBaseId());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContent(chunk.getContent());
        vo.setMetadata(chunk.getMetadata());
        vo.setCreatedAt(chunk.getCreatedAt());
        vo.setUpdatedAt(chunk.getUpdatedAt());

        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {

        this.updatedAt = updatedAt;
    }
}