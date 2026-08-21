package com.ljl.agent.ingestion;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.entity.Document;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.mapper.DocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 将状态更新限制在短事务中，文件读取和文本解析不占用数据库事务。
 */
@Service
public class DocumentIngestionPersistenceService {

    private final DocumentMapper documentMapper;

    public DocumentIngestionPersistenceService(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    @Transactional
    public Document createUploadedDocument(Document document) {
        if (documentMapper.insertUploaded(document) != 1) {
            throw new BusinessException(ErrorCode.DOCUMENT_INGESTION_FAILED);
        }
        Document saved = documentMapper.selectById(document.getId());
        if (saved == null) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_INGESTION_FAILED,
                    "上传文档元数据保存失败"
            );
        }
        return saved;
    }

    @Transactional
    public boolean markParseProcessing(
            Long documentId,
            Long currentUserId,
            boolean force
    ) {
        return documentMapper.markParseProcessing(
                documentId,
                currentUserId,
                force
        ) == 1;
    }

    @Transactional
    public void markParseSuccess(
            Long documentId,
            Long currentUserId,
            String content
    ) {
        if (documentMapper.updateParseSuccess(
                documentId,
                currentUserId,
                content
        ) != 1) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_INGESTION_FAILED,
                    "解析结果保存失败"
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markParseFailed(
            Long documentId,
            Long currentUserId,
            String safeError
    ) {
        return documentMapper.updateParseFailed(
                documentId,
                currentUserId,
                safeError
        ) == 1;
    }
}
