package com.ljl.agent.common;

/**
 * 项目统一业务错误码。
 *
 * <p>已有错误码保持原值，避免接口调用方因本次集中治理而失去兼容性。</p>
 */
public enum ErrorCode {

    PARAM_INVALID(40001, "参数不合法"),
    INVALID_USERNAME_OR_PASSWORD(40101, "用户名或密码错误"),
    AUTHENTICATION_REQUIRED(40102, "未认证或登录状态无效"),

    USER_DISABLED(40301, "用户已被禁用"),
    KNOWLEDGE_BASE_FORBIDDEN(40302, "知识库不属于当前用户"),
    ACCESS_DENIED(40303, "权限不足"),

    USER_NOT_FOUND(40401, "用户不存在"),
    KNOWLEDGE_BASE_NOT_FOUND(40402, "知识库不存在"),
    DOCUMENT_NOT_FOUND(40403, "文档不存在"),
    CHAT_SESSION_NOT_FOUND(40404, "聊天会话不存在"),

    USER_DUPLICATE(40901, "用户名或邮箱已存在"),
    KNOWLEDGE_BASE_DUPLICATE(40902, "该用户下已存在同名知识库"),
    DOCUMENT_DUPLICATE(40903, "该知识库下已存在同名文档"),
    DOCUMENT_CHUNK_DUPLICATE(40904, "该文档下已存在相同序号的切片"),
    CHAT_MESSAGE_DUPLICATE(40905, "requestId 已存在"),

    SYSTEM_ERROR(50000, "系统内部错误"),
    USER_CREATE_FAILED(50001, "用户创建失败"),
    KNOWLEDGE_BASE_CREATE_FAILED(50002, "知识库创建失败"),
    DOCUMENT_CREATE_FAILED(50003, "文档创建失败"),
    DOCUMENT_CHUNK_CREATE_FAILED(50004, "文档切片创建失败"),
    CHAT_SESSION_CREATE_FAILED(50005, "聊天会话创建失败"),
    CHAT_MESSAGE_CREATE_FAILED(50006, "聊天消息创建失败"),
    DOCUMENT_DELETE_FAILED(50007, "文档删除失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
