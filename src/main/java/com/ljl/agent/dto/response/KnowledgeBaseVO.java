package com.ljl.agent.dto.response;

import com.ljl.agent.entity.KnowledgeBase;

import java.time.LocalDateTime;

public class KnowledgeBaseVO {

    private Long id;

    private Long userId;

    private String name;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static KnowledgeBaseVO from(KnowledgeBase knowledgeBase) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();

        vo.setId(knowledgeBase.getId());
        vo.setUserId(knowledgeBase.getUserId());
        vo.setName(knowledgeBase.getName());
        vo.setDescription(knowledgeBase.getDescription());
        vo.setCreatedAt(knowledgeBase.getCreatedAt());
        vo.setUpdatedAt(knowledgeBase.getUpdatedAt());

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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