package com.ljl.agent.config;

import com.ljl.agent.ingestion.DocumentFileType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 文档上传与本地存储边界配置。
 */
@ConfigurationProperties(prefix = "app.document")
public final class DocumentIngestionProperties {

    private final Path storageRoot;
    private final long maxFileSizeBytes;
    private final Set<DocumentFileType> allowedTypes;

    public DocumentIngestionProperties(
            String storageRoot,
            DataSize maxFileSize,
            List<String> allowedTypes
    ) {
        if (storageRoot == null || storageRoot.isBlank()) {
            throw new IllegalArgumentException("文档存储根目录不能为空");
        }
        if (maxFileSize == null || maxFileSize.toBytes() <= 0) {
            throw new IllegalArgumentException("文档大小上限必须大于0");
        }
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            throw new IllegalArgumentException("文档允许类型不能为空");
        }

        EnumSet<DocumentFileType> parsedTypes =
                EnumSet.noneOf(DocumentFileType.class);
        for (String allowedType : allowedTypes) {
            parsedTypes.add(DocumentFileType.fromConfiguredName(allowedType));
        }

        this.storageRoot = Path.of(storageRoot.trim());
        this.maxFileSizeBytes = maxFileSize.toBytes();
        this.allowedTypes = Collections.unmodifiableSet(parsedTypes);
    }

    public Path getStorageRoot() {
        return storageRoot;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public Set<DocumentFileType> getAllowedTypes() {
        return allowedTypes;
    }

    public boolean isAllowed(DocumentFileType fileType) {
        return allowedTypes.contains(fileType);
    }
}
