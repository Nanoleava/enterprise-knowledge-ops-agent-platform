package com.ljl.agent.service.impl;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.common.PageResult;
import com.ljl.agent.dto.request.DocumentChunkCreateRequest;
import com.ljl.agent.dto.request.DocumentCreateRequest;
import com.ljl.agent.dto.request.DocumentPageQuery;
import com.ljl.agent.dto.response.DocumentChunkVO;
import com.ljl.agent.dto.response.DocumentVO;
import com.ljl.agent.entity.Document;
import com.ljl.agent.entity.DocumentChunk;
import com.ljl.agent.entity.KnowledgeBase;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.mapper.DocumentChunkMapper;
import com.ljl.agent.mapper.DocumentMapper;
import com.ljl.agent.mapper.KnowledgeBaseMapper;
import com.ljl.agent.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class DocumentServiceImpl implements DocumentService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public DocumentServiceImpl(
            DocumentMapper documentMapper,
            DocumentChunkMapper documentChunkMapper,
            KnowledgeBaseMapper knowledgeBaseMapper
    ) {
        this.documentMapper = documentMapper;
        this.documentChunkMapper = documentChunkMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    @Override
    @Transactional
    public DocumentVO create(DocumentCreateRequest request) {
        if (request == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "文档创建参数不能为空"
            );
        }

        Long userId = requirePositiveId(
                request.getUserId(),
                "用户ID必须是正整数"
        );
        Long knowledgeBaseId = requirePositiveId(
                request.getKnowledgeBaseId(),
                "知识库ID必须是正整数"
        );
        String title = requireText(
                request.getTitle(),
                "文档标题不能为空"
        );
        String content = requireContent(request.getContent());

        KnowledgeBase knowledgeBase =
                knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw knowledgeBaseNotFound(knowledgeBaseId);
        }

        if (!userId.equals(knowledgeBase.getUserId())) {
            throw new BusinessException(
                    ErrorCode.KNOWLEDGE_BASE_FORBIDDEN,
                    "知识库不属于当前用户，userId="
                            + userId
                            + ", knowledgeBaseId="
                            + knowledgeBaseId
            );
        }

        Document existing =
                documentMapper.selectByKnowledgeBaseIdAndTitle(
                        knowledgeBaseId,
                        title
                );
        if (existing != null) {
            throw duplicateDocument(knowledgeBaseId, title);
        }

        Document document = new Document();
        document.setUserId(userId);
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(title);
        // 文档正文可能包含代码缩进和换行，因此不调用 trim()。
        document.setContent(content);
        document.setStatus(Document.STATUS_ACTIVE);

        try {
            int affectedRows = documentMapper.insert(document);
            if (affectedRows != 1) {
                throw new BusinessException(
                        ErrorCode.DOCUMENT_CREATE_FAILED
                );
            }
        } catch (DuplicateKeyException exception) {
            // 数据库唯一索引用于处理并发创建同名文档的情况。
            throw duplicateDocument(knowledgeBaseId, title);
        }

        Document saved = documentMapper.selectById(document.getId());
        if (saved == null) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_CREATE_FAILED,
                    "文档创建后查询失败，documentId=" + document.getId()
            );
        }

        LOGGER.info(
                "document created: documentId={}, knowledgeBaseId={}, userId={}",
                saved.getId(),
                saved.getKnowledgeBaseId(),
                saved.getUserId()
        );

        return DocumentVO.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentVO> listAll() {
        return documentMapper.selectAll()
                .stream()
                .map(DocumentVO::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentVO> listByKnowledgeBaseId(Long knowledgeBaseId) {
        Long validKnowledgeBaseId = requirePositiveId(
                knowledgeBaseId,
                "知识库ID必须是正整数"
        );

        KnowledgeBase knowledgeBase =
                knowledgeBaseMapper.selectById(validKnowledgeBaseId);
        if (knowledgeBase == null) {
            throw knowledgeBaseNotFound(validKnowledgeBaseId);
        }

        return documentMapper
                .selectByKnowledgeBaseId(validKnowledgeBaseId)
                .stream()
                .map(DocumentVO::from)
                .toList();
    }

    @Override
    @Transactional
    public DocumentChunkVO createChunk(
            Long documentId,
            DocumentChunkCreateRequest request
    ) {
        Long validDocumentId = requirePositiveId(
                documentId,
                "文档ID必须是正整数"
        );
        if (request == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "文档切片创建参数不能为空"
            );
        }

        Integer chunkIndex = requireNonNegativeChunkIndex(
                request.getChunkIndex()
        );
        String content = requireChunkContent(request.getContent());

        Document document = requireDocument(validDocumentId);
        DocumentChunk existing =
                documentChunkMapper.selectByDocumentIdAndChunkIndex(
                        validDocumentId,
                        chunkIndex
                );
        if (existing != null) {
            throw duplicateDocumentChunk(validDocumentId, chunkIndex);
        }

        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(validDocumentId);
        // knowledgeBaseId 只从所属文档继承，避免客户端构造跨知识库切片。
        chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
        chunk.setChunkIndex(chunkIndex);
        // 切片正文可能包含代码缩进和换行，因此不调用 trim()。
        chunk.setContent(content);
        chunk.setMetadata(request.getMetadata());

        try {
            int affectedRows = documentChunkMapper.insert(chunk);
            if (affectedRows != 1) {
                throw new BusinessException(
                        ErrorCode.DOCUMENT_CHUNK_CREATE_FAILED
                );
            }
        } catch (DuplicateKeyException exception) {
            // 数据库唯一索引用于处理并发创建相同序号切片的情况。
            throw duplicateDocumentChunk(validDocumentId, chunkIndex);
        }

        DocumentChunk saved = documentChunkMapper.selectById(chunk.getId());
        if (saved == null) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_CHUNK_CREATE_FAILED,
                    "文档切片创建后查询失败，chunkId=" + chunk.getId()
            );
        }

        LOGGER.info(
                "document chunk created: chunkId={}, documentId={}, chunkIndex={}",
                saved.getId(),
                saved.getDocumentId(),
                saved.getChunkIndex()
        );

        return DocumentChunkVO.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkVO> listChunksByDocumentId(Long documentId) {
        Long validDocumentId = requirePositiveId(
                documentId,
                "文档ID必须是正整数"
        );
        requireDocument(validDocumentId);

        return documentChunkMapper.selectByDocumentId(validDocumentId)
                .stream()
                .map(DocumentChunkVO::from)
                .toList();
    }

    private BusinessException knowledgeBaseNotFound(Long knowledgeBaseId) {
        return new BusinessException(
                ErrorCode.KNOWLEDGE_BASE_NOT_FOUND,
                "知识库不存在，knowledgeBaseId=" + knowledgeBaseId
        );
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

    private Document requireDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_NOT_FOUND,
                    "文档不存在，documentId=" + documentId
            );
        }
        return document;
    }

    private BusinessException duplicateDocumentChunk(
            Long documentId,
            Integer chunkIndex
    ) {
        return new BusinessException(
                ErrorCode.DOCUMENT_CHUNK_DUPLICATE,
                "该文档下已存在相同序号的切片，documentId="
                        + documentId
                        + ", chunkIndex="
                        + chunkIndex
        );
    }

    private Long requirePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, message);
        }
        return id;
    }

    private String requireText(String text, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, message);
        }
        return text.trim();
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "文档内容不能为空"
            );
        }
        return content;
    }

    private Integer requireNonNegativeChunkIndex(Integer chunkIndex) {
        if (chunkIndex == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "切片序号不能为空"
            );
        }
        if (chunkIndex < 0) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "切片序号不能小于0"
            );
        }
        return chunkIndex;
    }

    private String requireChunkContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "切片内容不能为空"
            );
        }
        return content;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentVO getById(Long id) {
        Long validId = requirePositiveId(id, "文档ID必须是正整数");
        Document document = requireDocument(validId);

        return DocumentVO.from(document);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Long validId = requirePositiveId(id, "文档ID必须是正整数");
        Document document = requireDocument(validId);

        int affected = documentMapper.deleteById(validId);

        if (affected != 1) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_DELETE_FAILED
            );
        }

        LOGGER.info(
                "document deleted: documentId={}, knowledgeBaseId={}, userId={}",
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getUserId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DocumentVO> page(
            DocumentPageQuery query) {

        if (query == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "分页查询参数不能为空"
            );
        }

        int page = requirePageNumber(query.getPage());
        int size = requirePageSize(query.getSize());
        Long knowledgeBaseId = query.getKnowledgeBaseId();
        if (knowledgeBaseId != null) {
            requirePositiveId(
                    knowledgeBaseId,
                    "knowledgeBaseId必须为正数"
            );
        }

        String keyword = query.getKeyword();

        if (keyword != null) {
            keyword = keyword.trim();

            if (keyword.isEmpty()) {
                keyword = null;
            } else if (keyword.length() > 100) {
                throw new BusinessException(
                        ErrorCode.PARAM_INVALID,
                        "keyword长度不能超过100个字符"
                );
            }
        }

        long total = documentMapper.countPage(
                keyword,
                knowledgeBaseId
        );

        if (total == 0) {
            return new PageResult<>(
                    List.of(),
                    0,
                    page,
                    size
            );
        }

        List<Document> documents =
                documentMapper.selectPage(
                        keyword,
                        knowledgeBaseId,
                        (long) (page - 1) * size,
                        size
                );

        List<DocumentVO> records =
                documents.stream()
                        .map(DocumentVO::from)
                        .toList();

        return new PageResult<>(
                records,
                total,
                page,
                size
        );
    }

    private int requirePageNumber(Integer page) {
        if (page == null || page < 1) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "page不能小于1"
            );
        }
        return page;
    }

    private int requirePageSize(Integer size) {
        if (size == null || size < 1) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "size不能小于1"
            );
        }
        if (size > 100) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "size不能超过100"
            );
        }
        return size;
    }

}
