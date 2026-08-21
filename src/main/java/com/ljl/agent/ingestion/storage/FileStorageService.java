package com.ljl.agent.ingestion.storage;

import com.ljl.agent.ingestion.ValidatedUpload;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {

    StoredFile save(
            Long userId,
            Long knowledgeBaseId,
            MultipartFile file,
            ValidatedUpload validatedUpload
    );

    Path resolveForRead(String relativePath);

    void deleteIfExists(String relativePath);
}
