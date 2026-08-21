package com.ljl.agent.ingestion;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.dto.response.DocumentProcessStatusVO;
import com.ljl.agent.dto.response.DocumentUploadResponse;
import com.ljl.agent.entity.Document;
import com.ljl.agent.entity.KnowledgeBase;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.ingestion.clean.TextCleaner;
import com.ljl.agent.ingestion.parser.DocumentParsingException;
import com.ljl.agent.ingestion.parser.ParserRegistry;
import com.ljl.agent.ingestion.storage.DocumentStorageException;
import com.ljl.agent.ingestion.storage.FileStorageService;
import com.ljl.agent.ingestion.storage.StoredFile;
import com.ljl.agent.mapper.DocumentChunkMapper;
import com.ljl.agent.mapper.DocumentMapper;
import com.ljl.agent.mapper.KnowledgeBaseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Service
public class DefaultDocumentIngestionService
        implements DocumentIngestionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DefaultDocumentIngestionService.class);
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_ERROR_LENGTH = 500;

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentUploadValidator uploadValidator;
    private final FileStorageService fileStorageService;
    private final ParserRegistry parserRegistry;
    private final TextCleaner textCleaner;
    private final DocumentIngestionPersistenceService persistenceService;

    public DefaultDocumentIngestionService(
            KnowledgeBaseMapper knowledgeBaseMapper,
            DocumentMapper documentMapper,
            DocumentChunkMapper documentChunkMapper,
            DocumentUploadValidator uploadValidator,
            FileStorageService fileStorageService,
            ParserRegistry parserRegistry,
            TextCleaner textCleaner,
            DocumentIngestionPersistenceService persistenceService
    ) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.documentMapper = documentMapper;
        this.documentChunkMapper = documentChunkMapper;
        this.uploadValidator = uploadValidator;
        this.fileStorageService = fileStorageService;
        this.parserRegistry = parserRegistry;
        this.textCleaner = textCleaner;
        this.persistenceService = persistenceService;
    }

    @Override
    public DocumentUploadResponse upload(
            Long currentUserId,
            Long knowledgeBaseId,
            MultipartFile file,
            String requestedTitle
    ) {
        Long userId = requirePositive(currentUserId, "当前用户ID");
        Long validKnowledgeBaseId = requirePositive(
                knowledgeBaseId,
                "知识库ID"
        );

        // owner 校验必须早于文件内容读取和任何磁盘写入。
        requireOwnedKnowledgeBase(userId, validKnowledgeBaseId);

        ValidatedUpload upload = uploadValidator.validate(file);
        String title = resolveTitle(
                requestedTitle,
                upload.originalFileName()
        );
        if (documentMapper.selectByKnowledgeBaseIdAndTitle(
                validKnowledgeBaseId,
                title
        ) != null) {
            throw duplicateDocument(validKnowledgeBaseId, title);
        }

        StoredFile storedFile;
        try {
            storedFile = fileStorageService.save(
                    userId,
                    validKnowledgeBaseId,
                    file,
                    upload
            );
        } catch (DocumentStorageException exception) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_INGESTION_FAILED,
                    "文档文件存储失败",
                    exception
            );
        }

        Document document = uploadedDocument(
                userId,
                validKnowledgeBaseId,
                title,
                upload,
                storedFile
        );
        try {
            Document saved =
                    persistenceService.createUploadedDocument(document);
            LOGGER.info(
                    "document uploaded: documentId={}, knowledgeBaseId={}, userId={}, fileType={}, fileSize={}",
                    saved.getId(),
                    saved.getKnowledgeBaseId(),
                    saved.getUserId(),
                    saved.getFileType(),
                    saved.getFileSize()
            );
            return DocumentUploadResponse.from(saved);
        } catch (DuplicateKeyException exception) {
            compensateStoredFile(storedFile.relativePath());
            throw duplicateDocument(validKnowledgeBaseId, title);
        } catch (RuntimeException exception) {
            compensateStoredFile(storedFile.relativePath());
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(
                    ErrorCode.DOCUMENT_INGESTION_FAILED,
                    "上传文档元数据保存失败",
                    exception
            );
        }
    }

    @Override
    public DocumentProcessStatusVO parse(
            Long currentUserId,
            Long documentId,
            boolean force
    ) {
        Long userId = requirePositive(currentUserId, "当前用户ID");
        Long validDocumentId = requirePositive(documentId, "文档ID");

        // owner 校验必须早于 storage.resolveForRead。
        Document document = requireOwnedDocument(userId, validDocumentId);
        requireFileBackedDocument(document);

        if (Document.PROCESS_PROCESSING.equals(document.getParseStatus())
                || (Document.PROCESS_SUCCESS.equals(document.getParseStatus())
                && !force)) {
            throw processingConflict();
        }
        if (!persistenceService.markParseProcessing(
                validDocumentId,
                userId,
                force
        )) {
            throw processingConflict();
        }

        String cleanedText;
        try {
            DocumentFileType fileType =
                    DocumentFileType.fromConfiguredName(document.getFileType());
            Path path = fileStorageService.resolveForRead(
                    document.getFilePath()
            );
            String parsedText = parserRegistry
                    .requireParser(fileType)
                    .parse(path);
            cleanedText = textCleaner.clean(parsedText);
        } catch (DocumentParsingException exception) {
            throw parseFailed(
                    validDocumentId,
                    userId,
                    safeSummary(exception.getMessage()),
                    exception
            );
        } catch (DocumentStorageException exception) {
            throw parseFailed(
                    validDocumentId,
                    userId,
                    "文档源文件不可用，无法解析",
                    exception
            );
        } catch (IllegalArgumentException exception) {
            throw parseFailed(
                    validDocumentId,
                    userId,
                    "文档文件类型无法解析",
                    exception
            );
        }

        try {
            persistenceService.markParseSuccess(
                    validDocumentId,
                    userId,
                    cleanedText
            );
        } catch (RuntimeException exception) {
            markParseFailedSafely(
                    validDocumentId,
                    userId,
                    "解析结果保存失败，请重试"
            );
            throw new BusinessException(
                    ErrorCode.DOCUMENT_INGESTION_FAILED,
                    "解析结果保存失败，请重试",
                    exception
            );
        }

        LOGGER.info(
                "document parsed: documentId={}, userId={}, fileType={}, contentLength={}",
                validDocumentId,
                userId,
                document.getFileType(),
                cleanedText.length()
        );
        return getStatus(userId, validDocumentId);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentProcessStatusVO getStatus(
            Long currentUserId,
            Long documentId
    ) {
        Long userId = requirePositive(currentUserId, "当前用户ID");
        Long validDocumentId = requirePositive(documentId, "文档ID");
        Document document = requireOwnedDocument(userId, validDocumentId);
        long chunkCount = documentChunkMapper.countByDocumentId(
                validDocumentId
        );
        return DocumentProcessStatusVO.from(document, chunkCount);
    }

    private Document uploadedDocument(
            Long userId,
            Long knowledgeBaseId,
            String title,
            ValidatedUpload upload,
            StoredFile storedFile
    ) {
        Document document = new Document();
        document.setUserId(userId);
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(title);
        document.setContent("");
        document.setStatus(Document.STATUS_ACTIVE);
        document.setOriginalFileName(upload.originalFileName());
        document.setStoredFileName(storedFile.storedFileName());
        document.setFileType(storedFile.fileType().name());
        document.setFileSize(storedFile.fileSize());
        document.setFilePath(storedFile.relativePath());
        document.setFileChecksum(storedFile.sha256());
        document.setParseStatus(Document.PROCESS_PENDING);
        document.setChunkStatus(Document.PROCESS_PENDING);
        return document;
    }

    private KnowledgeBase requireOwnedKnowledgeBase(
            Long userId,
            Long knowledgeBaseId
    ) {
        KnowledgeBase knowledgeBase =
                knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException(
                    ErrorCode.KNOWLEDGE_BASE_NOT_FOUND,
                    "知识库不存在，knowledgeBaseId=" + knowledgeBaseId
            );
        }
        if (!userId.equals(knowledgeBase.getUserId())) {
            throw new BusinessException(
                    ErrorCode.KNOWLEDGE_BASE_FORBIDDEN,
                    "知识库不属于当前用户"
            );
        }
        return knowledgeBase;
    }

    private Document requireOwnedDocument(Long userId, Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_NOT_FOUND,
                    "文档不存在，documentId=" + documentId
            );
        }
        if (!userId.equals(document.getUserId())) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_FORBIDDEN,
                    "文档不属于当前用户"
            );
        }
        return document;
    }

    private void requireFileBackedDocument(Document document) {
        if (document.getFilePath() == null
                || document.getFileType() == null
                || Document.PROCESS_NOT_APPLICABLE.equals(
                document.getParseStatus()
        )) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_FILE_INVALID,
                    "该文档不是通过文件上传创建，不能执行解析"
            );
        }
    }

    private String resolveTitle(String requestedTitle, String fileName) {
        String title = requestedTitle == null
                ? ""
                : requestedTitle.trim();
        if (title.isEmpty()) {
            int extensionSeparator = fileName.lastIndexOf('.');
            title = extensionSeparator > 0
                    ? fileName.substring(0, extensionSeparator)
                    : fileName;
        }
        if (title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "文档标题不能为空且长度不能超过200个字符"
            );
        }
        return title;
    }

    private BusinessException duplicateDocument(
            Long knowledgeBaseId,
            String title
    ) {
        return new BusinessException(
                ErrorCode.DOCUMENT_DUPLICATE,
                "该知识库下已存在同名文档，knowledgeBaseId="
                        + knowledgeBaseId
                        + ", title="
                        + title
        );
    }

    private BusinessException processingConflict() {
        return new BusinessException(ErrorCode.DOCUMENT_PROCESSING_CONFLICT);
    }

    private BusinessException parseFailed(
            Long documentId,
            Long userId,
            String safeError,
            RuntimeException cause
    ) {
        markParseFailedSafely(documentId, userId, safeError);
        return new BusinessException(
                ErrorCode.DOCUMENT_PARSE_FAILED,
                safeError,
                cause
        );
    }

    private void markParseFailedSafely(
            Long documentId,
            Long userId,
            String safeError
    ) {
        try {
            if (!persistenceService.markParseFailed(
                    documentId,
                    userId,
                    safeSummary(safeError)
            )) {
                LOGGER.error(
                        "parse failure state was not updated: documentId={}, userId={}",
                        documentId,
                        userId
                );
            }
        } catch (RuntimeException statusException) {
            LOGGER.error(
                    "parse failure state update failed: documentId={}, userId={}, exceptionType={}",
                    documentId,
                    userId,
                    statusException.getClass().getSimpleName()
            );
        }
    }

    private String safeSummary(String message) {
        String value = message == null || message.isBlank()
                ? "文档解析失败，请检查文件编码和内容"
                : message.trim();
        return value.length() <= MAX_ERROR_LENGTH
                ? value
                : value.substring(0, MAX_ERROR_LENGTH);
    }

    private void compensateStoredFile(String relativePath) {
        try {
            fileStorageService.deleteIfExists(relativePath);
        } catch (RuntimeException cleanupException) {
            LOGGER.error(
                    "orphan upload compensation failed: exceptionType={}",
                    cleanupException.getClass().getSimpleName()
            );
        }
    }

    private Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    fieldName + "必须是正整数"
            );
        }
        return value;
    }
}
