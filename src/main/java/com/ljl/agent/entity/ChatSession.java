package com.ljl.agent.entity;

public class ChatSession extends BaseEntity {

    private Long userId;
    private String title;

    public ChatSession() {
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
}