package com.ljl.agent.service;

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
import com.ljl.agent.mapper.DocumentMapper;
import com.ljl.agent.mapper.DocumentChunkMapper;
import com.ljl.agent.mapper.KnowledgeBaseMapper;
import com.ljl.agent.service.impl.DocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class DocumentServiceImplTest {

    private DocumentMapper documentMapper;
    private DocumentChunkMapper documentChunkMapper;
    private KnowledgeBaseMapper knowledgeBaseMapper;
    private DocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        documentMapper = mock(DocumentMapper.class);
        documentChunkMapper = mock(DocumentChunkMapper.class);
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        service = new DocumentServiceImpl(
                documentMapper,
                documentChunkMapper,
                knowledgeBaseMapper
        );
    }

    @Test
    void shouldRejectMissingKnowledgeBase() {
        DocumentCreateRequest request = request(
                7L,
                20L,
                "入门",
                "正文"
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.create(7L, request)
        );

        assertEquals(40402, exception.getCode());
        assertEquals(
                "知识库不存在，knowledgeBaseId=20",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectKnowledgeBaseOwnedByAnotherUser() {
        DocumentCreateRequest request = request(
                8L,
                20L,
                "入门",
                "正文"
        );
        when(knowledgeBaseMapper.selectById(20L))
                .thenReturn(knowledgeBase(20L, 7L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.create(8L, request)
        );

        assertEquals(40302, exception.getCode());
    }

    @Test
    void shouldHandleConcurrentDuplicateWithConflictCode() {
        DocumentCreateRequest request = request(
                7L,
                20L,
                "入门",
                "正文"
        );
        when(knowledgeBaseMapper.selectById(20L))
                .thenReturn(knowledgeBase(20L, 7L));
        when(documentMapper.insert(any(Document.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.create(7L, request)
        );

        assertEquals(40903, exception.getCode());
    }

    @Test
    void shouldNormalizeTitleAndPreserveDocumentContent(
            CapturedOutput output
    ) {
        String content = "  第一行\n    code();\n";
        DocumentCreateRequest request = request(
                7L,
                20L,
                "  入门  ",
                content
        );
        AtomicReference<Document> inserted = new AtomicReference<>();
        when(knowledgeBaseMapper.selectById(20L))
                .thenReturn(knowledgeBase(20L, 7L));
        when(documentMapper.insert(any(Document.class)))
                .thenAnswer(invocation -> {
                    Document value = invocation.getArgument(0);
                    value.setId(30L);
                    inserted.set(value);
                    return 1;
                });
        when(documentMapper.selectById(30L)).thenAnswer(invocation -> {
            Document value = inserted.get();
            value.setCreatedAt(LocalDateTime.now());
            value.setUpdatedAt(LocalDateTime.now());
            return value;
        });

        DocumentVO result = service.create(7L, request);

        assertEquals("入门", inserted.get().getTitle());
        assertEquals(content, inserted.get().getContent());
        assertEquals(Document.STATUS_ACTIVE, inserted.get().getStatus());
        assertEquals(content, result.getContent());
        assertTrue(output.getAll().contains(
                "document created: documentId=30, knowledgeBaseId=20, userId=7"
        ));
        assertFalse(output.getAll().contains(content));
    }

    @Test
    void shouldRejectInvalidKnowledgeBaseIdWhenListing() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.listByKnowledgeBaseId(7L, 0L)
        );

        assertEquals(40001, exception.getCode());
        assertEquals(
                "知识库ID必须是正整数",
                exception.getMessage()
        );
    }

    @Test
    void createShouldHaveTransactionBoundary() throws Exception {
        Transactional transactional = DocumentServiceImpl.class
                .getMethod(
                        "create",
                        Long.class,
                        DocumentCreateRequest.class
                )
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
    }

    @Test
    void shouldCreateChunkWithKnowledgeBaseInheritedFromDocument(
            CapturedOutput output
    ) {
        String content = "  第一行\n    code();\n";
        DocumentChunkCreateRequest request = chunkRequest(
                0,
                content,
                "{\"source\":\"manual\"}"
        );
        Document document = document(30L, 20L);
        AtomicReference<DocumentChunk> inserted = new AtomicReference<>();
        when(documentMapper.selectById(30L)).thenReturn(document);
        when(documentChunkMapper.insert(any(DocumentChunk.class)))
                .thenAnswer(invocation -> {
                    DocumentChunk value = invocation.getArgument(0);
                    value.setId(40L);
                    inserted.set(value);
                    return 1;
                });
        when(documentChunkMapper.selectById(40L)).thenAnswer(invocation -> {
            DocumentChunk value = inserted.get();
            value.setCreatedAt(LocalDateTime.now());
            value.setUpdatedAt(LocalDateTime.now());
            return value;
        });

        DocumentChunkVO result = service.createChunk(7L, 30L, request);

        assertEquals(30L, inserted.get().getDocumentId());
        assertEquals(20L, inserted.get().getKnowledgeBaseId());
        assertEquals(content, inserted.get().getContent());
        assertEquals(request.getMetadata(), inserted.get().getMetadata());
        assertEquals(40L, result.getId());
        assertEquals(content, result.getContent());
        assertTrue(output.getAll().contains(
                "document chunk created: chunkId=40, documentId=30, chunkIndex=0"
        ));
        assertFalse(output.getAll().contains(content));
    }

    @Test
    void userBCannotCreateChunkUnderUserADocumentAndInsertDoesNotRun() {
        when(documentMapper.selectById(30L))
                .thenReturn(document(30L, 20L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createChunk(
                        8L,
                        30L,
                        chunkRequest(0, "越权内容", null)
                )
        );

        assertEquals(40304, exception.getCode());
        verifyNoInteractions(documentChunkMapper);
    }

    @Test
    void shouldRejectChunkWhenDocumentDoesNotExist() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createChunk(
                        7L,
                        30L,
                        chunkRequest(0, "正文", null)
                )
        );

        assertEquals(40403, exception.getCode());
        assertEquals("文档不存在，documentId=30", exception.getMessage());
        verifyNoInteractions(documentChunkMapper);
    }

    @Test
    void shouldRejectExistingChunkIndex() {
        when(documentMapper.selectById(30L))
                .thenReturn(document(30L, 20L));
        when(documentChunkMapper.selectByDocumentIdAndChunkIndex(30L, 2))
                .thenReturn(chunk(41L, 30L, 20L, 2, "已有内容"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createChunk(
                        7L,
                        30L,
                        chunkRequest(2, "新内容", null)
                )
        );

        assertEquals(40904, exception.getCode());
    }

    @Test
    void shouldHandleConcurrentDuplicateChunkWithConflictCode() {
        when(documentMapper.selectById(30L))
                .thenReturn(document(30L, 20L));
        when(documentChunkMapper.insert(any(DocumentChunk.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createChunk(
                        7L,
                        30L,
                        chunkRequest(2, "正文", null)
                )
        );

        assertEquals(40904, exception.getCode());
    }

    @Test
    void shouldListChunksForExistingDocument() {
        when(documentMapper.selectById(30L))
                .thenReturn(document(30L, 20L));
        when(documentChunkMapper.selectByDocumentId(30L))
                .thenReturn(List.of(
                        chunk(40L, 30L, 20L, 0, "第一段"),
                        chunk(41L, 30L, 20L, 1, "第二段")
                ));

        List<DocumentChunkVO> result =
                service.listChunksByDocumentId(7L, 30L);

        assertEquals(2, result.size());
        assertEquals(0, result.getFirst().getChunkIndex());
        assertEquals(1, result.getLast().getChunkIndex());
    }

    @Test
    void createChunkShouldHaveTransactionBoundary() throws Exception {
        Transactional transactional = DocumentServiceImpl.class
                .getMethod(
                        "createChunk",
                        Long.class,
                        Long.class,
                        DocumentChunkCreateRequest.class
                )
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
    }

    @Test
    void shouldGetDocumentById() {
        when(documentMapper.selectById(30L))
                .thenReturn(document(30L, 20L));

        DocumentVO result = service.getById(7L, 30L);

        assertEquals(30L, result.getId());
        assertEquals(20L, result.getKnowledgeBaseId());
        assertEquals("文档30", result.getTitle());
    }

    @Test
    void shouldReturnNotFoundWhenGettingMissingDocument() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getById(7L, 30L)
        );

        assertEquals(40403, exception.getCode());
        assertEquals("文档不存在，documentId=30", exception.getMessage());
    }

    @Test
    void shouldDeleteExistingDocumentUsingDatabaseCascade() {
        when(documentMapper.selectById(30L))
                .thenReturn(document(30L, 20L));
        when(documentMapper.deleteByIdAndUserId(30L, 7L)).thenReturn(1);

        service.deleteById(7L, 30L);

        verify(documentMapper).deleteByIdAndUserId(30L, 7L);
        verifyNoInteractions(documentChunkMapper);
    }

    @Test
    void userBCannotReadOrDeleteUserADocument() {
        when(documentMapper.selectById(30L))
                .thenReturn(document(30L, 20L));

        BusinessException readException = assertThrows(
                BusinessException.class,
                () -> service.getById(8L, 30L)
        );
        BusinessException deleteException = assertThrows(
                BusinessException.class,
                () -> service.deleteById(8L, 30L)
        );

        assertEquals(40304, readException.getCode());
        assertEquals(40304, deleteException.getCode());
        verify(documentMapper, never())
                .deleteByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void shouldFailWhenDocumentDeleteAffectsNoRow() {
        when(documentMapper.selectById(30L))
                .thenReturn(document(30L, 20L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.deleteById(7L, 30L)
        );

        assertEquals(50007, exception.getCode());
        assertEquals("文档删除失败", exception.getMessage());
    }

    @Test
    void deleteShouldHaveTransactionBoundary() throws Exception {
        Transactional transactional = DocumentServiceImpl.class
                .getMethod("deleteById", Long.class, Long.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(false, transactional.readOnly());
    }

    @Test
    void shouldPageDocumentsWithTrimmedDynamicFilters() {
        DocumentPageQuery query = pageQuery(2, 5, "  Java  ", 20L);
        when(knowledgeBaseMapper.selectById(20L))
                .thenReturn(knowledgeBase(20L, 7L));
        when(documentMapper.countPage(7L, "Java", 20L)).thenReturn(8L);
        when(documentMapper.selectPage(7L, "Java", 20L, 5L, 5))
                .thenReturn(List.of(
                        document(31L, 20L),
                        document(30L, 20L)
                ));

        PageResult<DocumentVO> result = service.page(7L, query);

        assertEquals(8L, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(2L, result.getTotalPages());
        assertEquals(2, result.getRecords().size());
        assertEquals(31L, result.getRecords().getFirst().getId());
        verify(documentMapper).countPage(7L, "Java", 20L);
        verify(documentMapper).selectPage(7L, "Java", 20L, 5L, 5);
    }

    @Test
    void shouldSkipPageQueryWhenNoDocumentMatches() {
        DocumentPageQuery query = pageQuery(1, 10, "   ", null);
        when(documentMapper.countPage(7L, null, null)).thenReturn(0L);

        PageResult<DocumentVO> result = service.page(7L, query);

        assertEquals(0L, result.getTotal());
        assertEquals(0L, result.getTotalPages());
        assertEquals(List.of(), result.getRecords());
        verify(documentMapper, never()).selectPage(
                any(), any(), any(), anyLong(), anyInt()
        );
    }

    @Test
    void shouldValidatePageQueryWhenServiceIsCalledDirectly() {
        DocumentPageQuery invalidSize = pageQuery(1, 101, null, null);
        BusinessException sizeException = assertThrows(
                BusinessException.class,
                () -> service.page(7L, invalidSize)
        );

        DocumentPageQuery invalidKeyword = pageQuery(
                1,
                10,
                "x".repeat(101),
                null
        );
        BusinessException keywordException = assertThrows(
                BusinessException.class,
                () -> service.page(7L, invalidKeyword)
        );

        assertEquals(40001, sizeException.getCode());
        assertEquals("size不能超过100", sizeException.getMessage());
        assertEquals(40001, keywordException.getCode());
        assertEquals(
                "keyword长度不能超过100个字符",
                keywordException.getMessage()
        );
        verifyNoInteractions(knowledgeBaseMapper, documentChunkMapper);
    }

    @Test
    void pageShouldUseReadOnlyTransaction() throws Exception {
        Transactional transactional = DocumentServiceImpl.class
                .getMethod("page", Long.class, DocumentPageQuery.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(true, transactional.readOnly());
    }

    private DocumentCreateRequest request(
            Long userId,
            Long knowledgeBaseId,
            String title,
            String content
    ) {
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setTitle(title);
        request.setContent(content);
        return request;
    }

    private KnowledgeBase knowledgeBase(Long id, Long userId) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setUserId(userId);
        return knowledgeBase;
    }

    private Document document(Long id, Long knowledgeBaseId) {
        Document document = new Document();
        document.setId(id);
        document.setUserId(7L);
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle("文档" + id);
        document.setContent("正文" + id);
        document.setStatus(Document.STATUS_ACTIVE);
        return document;
    }

    private DocumentPageQuery pageQuery(
            Integer page,
            Integer size,
            String keyword,
            Long knowledgeBaseId
    ) {
        DocumentPageQuery query = new DocumentPageQuery();
        query.setPage(page);
        query.setSize(size);
        query.setKeyword(keyword);
        query.setKnowledgeBaseId(knowledgeBaseId);
        return query;
    }

    private DocumentChunkCreateRequest chunkRequest(
            Integer chunkIndex,
            String content,
            String metadata
    ) {
        DocumentChunkCreateRequest request =
                new DocumentChunkCreateRequest();
        request.setChunkIndex(chunkIndex);
        request.setContent(content);
        request.setMetadata(metadata);
        return request;
    }

    private DocumentChunk chunk(
            Long id,
            Long documentId,
            Long knowledgeBaseId,
            Integer chunkIndex,
            String content
    ) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setKnowledgeBaseId(knowledgeBaseId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        return chunk;
    }
}
