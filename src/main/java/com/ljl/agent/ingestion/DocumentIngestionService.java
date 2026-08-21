package com.ljl.agent.ingestion;

import com.ljl.agent.dto.response.DocumentProcessStatusVO;
import com.ljl.agent.dto.response.DocumentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentIngestionService {

    DocumentUploadResponse upload(
            Long currentUserId,
            Long knowledgeBaseId,
            MultipartFile file,
            String title
    );

    DocumentProcessStatusVO parse(
            Long currentUserId,
            Long documentId,
            boolean force
    );

    DocumentProcessStatusVO getStatus(
            Long currentUserId,
            Long documentId
    );
}
