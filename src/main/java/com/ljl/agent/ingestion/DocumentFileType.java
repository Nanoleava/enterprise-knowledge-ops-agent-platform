package com.ljl.agent.ingestion;

import java.util.Locale;
import java.util.Set;

/**
 * DAY 1 支持的文件格式及其基础声明类型。
 */
public enum DocumentFileType {

    TXT(
            Set.of("txt"),
            Set.of("text/plain", "application/octet-stream")
    ),
    MARKDOWN(
            Set.of("md", "markdown"),
            Set.of(
                    "text/markdown",
                    "text/x-markdown",
                    "text/plain",
                    "application/octet-stream"
            )
    );

    private final Set<String> extensions;
    private final Set<String> mediaTypes;

    DocumentFileType(Set<String> extensions, Set<String> mediaTypes) {
        this.extensions = extensions;
        this.mediaTypes = mediaTypes;
    }

    public static DocumentFileType fromConfiguredName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("文档类型配置不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "不支持的文档类型配置：" + value,
                    exception
            );
        }
    }

    public static DocumentFileType fromExtension(String extension) {
        if (extension == null) {
            return null;
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        for (DocumentFileType type : values()) {
            if (type.extensions.contains(normalized)) {
                return type;
            }
        }
        return null;
    }

    public boolean acceptsMediaType(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) {
            return true;
        }
        String normalized = mediaType
                .split(";", 2)[0]
                .trim()
                .toLowerCase(Locale.ROOT);
        return mediaTypes.contains(normalized);
    }

    public String canonicalExtension() {
        return this == TXT ? "txt" : "md";
    }
}
