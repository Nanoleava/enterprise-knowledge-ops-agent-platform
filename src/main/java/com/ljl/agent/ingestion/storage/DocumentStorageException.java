package com.ljl.agent.ingestion.storage;

/**
 * 文件系统内部失败。异常消息不得包含绝对路径。
 */
public class DocumentStorageException extends RuntimeException {

    public DocumentStorageException(String message) {
        super(message);
    }

    public DocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
