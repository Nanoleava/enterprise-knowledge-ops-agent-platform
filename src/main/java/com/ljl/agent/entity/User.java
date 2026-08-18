package com.ljl.agent.entity;

/**
 * 用户数据库实体。
 *
 * <p>字段与 MySQL user 表对应。
 * 不能直接作为 Controller 的请求体或响应体。</p>
 */
public class User extends BaseEntity {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    private String username;
    private String passwordHash;
    private String email;
    private String role;
    private Integer status;

    public User() {
        // MyBatis 通过无参构造方法创建对象。
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isNormalUser() {
        return ROLE_USER.equals(role);
    }
}