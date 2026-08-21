package com.ljl.agent.dto.response;

import com.ljl.agent.entity.Document;

import java.time.LocalDateTime;

public class DocumentVO {

    private Long id;

    private Long userId;

    private Long knowledgeBaseId;

    private String title;

    private String content;

    private String status;

    private String originalFileName;

    private String fileType;

    private Long fileSize;

    private String parseStatus;

    private String chunkStatus;

    private String processError;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static DocumentVO from(Document document) {
        DocumentVO vo = new DocumentVO();

        vo.setId(document.getId());
        vo.setUserId(document.getUserId());
        vo.setKnowledgeBaseId(document.getKnowledgeBaseId());
        vo.setTitle(document.getTitle());
        vo.setContent(document.getContent());
        vo.setStatus(document.getStatus());
        vo.setOriginalFileName(document.getOriginalFileName());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setParseStatus(document.getParseStatus());
        vo.setChunkStatus(document.getChunkStatus());
        vo.setProcessError(document.getProcessError());
        vo.setProcessedAt(document.getProcessedAt());
        vo.setCreatedAt(document.getCreatedAt());
        vo.setUpdatedAt(document.getUpdatedAt());

        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getChunkStatus() {
        return chunkStatus;
    }

    public void setChunkStatus(String chunkStatus) {
        this.chunkStatus = chunkStatus;
    }

    public String getProcessError() {
        return processError;
    }

    public void setProcessError(String processError) {
        this.processError = processError;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
