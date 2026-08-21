package com.ljl.agent.ingestion.parser;

/**
 * 可安全映射为解析失败摘要的格式异常。
 */
public class DocumentParsingException extends RuntimeException {

    public DocumentParsingException(String message) {
        super(message);
    }

    public DocumentParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
