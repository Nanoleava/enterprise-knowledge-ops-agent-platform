package com.ljl.agent.dto.response;

import com.ljl.agent.entity.ChatMessage;

import java.time.LocalDateTime;

public class ChatMessageVO {

    private Long id;
    private Long sessionId;
    private Long userId;
    private String role;
    private String content;
    private String requestId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatMessageVO from(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setUserId(message.getUserId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setRequestId(message.getRequestId());
        vo.setCreatedAt(message.getCreatedAt());
        vo.setUpdatedAt(message.getUpdatedAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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