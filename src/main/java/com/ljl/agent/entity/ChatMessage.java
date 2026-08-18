package com.ljl.agent.entity;

public class ChatMessage extends BaseEntity {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ASSISTANT = "ASSISTANT";
    public static final String ROLE_SYSTEM = "SYSTEM";

    private Long sessionId;
    private Long userId;
    private String role;
    private String content;
    private String requestId;

    public ChatMessage() {
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
}