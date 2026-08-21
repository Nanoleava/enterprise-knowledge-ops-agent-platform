package com.ljl.agent.ingestion;

/**
 * 已通过边界校验、可以进入存储层的上传元数据。
 */
public record ValidatedUpload(
        String originalFileName,
        DocumentFileType fileType,
        long fileSize
) {
}
