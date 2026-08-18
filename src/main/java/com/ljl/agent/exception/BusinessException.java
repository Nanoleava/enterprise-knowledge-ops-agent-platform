package com.ljl.agent.exception;

import com.ljl.agent.common.ErrorCode;

/**
 * 表示可以预期的业务失败。
 *
 * 例如：
 * 用户不存在、用户名重复、文档不存在。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    public BusinessException(
            ErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.code = errorCode.getCode();
    }
}
