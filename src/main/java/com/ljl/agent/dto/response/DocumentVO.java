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