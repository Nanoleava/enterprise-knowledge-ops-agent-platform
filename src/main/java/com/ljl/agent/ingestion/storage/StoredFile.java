package com.ljl.agent.ingestion.storage;

import com.ljl.agent.ingestion.DocumentFileType;

/**
 * 存储层返回的受控相对路径与审计元数据。
 */
public record StoredFile(
        String storedFileName,
        DocumentFileType fileType,
        long fileSize,
        String relativePath,
        String sha256
) {
}
