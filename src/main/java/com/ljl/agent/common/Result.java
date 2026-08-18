package com.ljl.agent.common;

/**
 * 统一接口响应对象。
 *
 * @param <T> data 字段的数据类型
 */
public class Result<T> {

    /**
     * 业务结果码。
     * 0 表示成功，非 0 表示失败。
     */
    private final int code;

    /**
     * 结果说明。
     */
    private final String message;

    /**
     * 实际响应数据。
     */
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    /**
     * 创建失败响应。
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 使用项目统一错误码创建失败响应。
     */
    public static <T> Result<T> failure(ErrorCode errorCode) {
        return new Result<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }

    /**
     * 创建带错误详情的失败响应。
     */
    public static <T> Result<T> failure(
            int code,
            String message,
            T data
    ) {
        return new Result<>(code, message, data);
    }

    /**
     * 使用项目统一错误码和错误详情创建失败响应。
     */
    public static <T> Result<T> failure(
            ErrorCode errorCode,
            T data
    ) {
        return new Result<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                data
        );
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
