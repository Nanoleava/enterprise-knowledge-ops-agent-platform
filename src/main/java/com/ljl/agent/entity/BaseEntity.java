package com.ljl.agent.entity;

import java.time.LocalDateTime;

/**
 * 数据库实体公共字段。
 *
 * <p>阶段 1 的 BaseEntity 会在 JVM 内存中生成 ID，
 * 阶段 2 的 ID 和时间改由 MySQL 管理。</p>
 */
public abstract class BaseEntity {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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