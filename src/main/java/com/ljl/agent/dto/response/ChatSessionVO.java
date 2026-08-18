package com.ljl.agent.dto.response;

import com.ljl.agent.entity.ChatSession;

import java.time.LocalDateTime;

public class ChatSessionVO {

    private Long id;
    private Long userId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatSessionVO from(ChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setTitle(session.getTitle());
        vo.setCreatedAt(session.getCreatedAt());
        vo.setUpdatedAt(session.getUpdatedAt());
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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