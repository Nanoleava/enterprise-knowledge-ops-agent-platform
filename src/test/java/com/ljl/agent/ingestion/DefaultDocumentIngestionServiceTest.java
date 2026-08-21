package com.ljl.agent.ingestion;

import com.ljl.agent.dto.response.DocumentProcessStatusVO;
import com.ljl.agent.entity.Document;
import com.ljl.agent.entity.KnowledgeBase;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.ingestion.clean.TextCleaner;
import com.ljl.agent.ingestion.parser.DocumentParser;
import com.ljl.agent.ingestion.parser.DocumentParsingException;
import com.ljl.agent.ingestion.parser.ParserRegistry;
import com.ljl.agent.ingestion.storage.FileStorageService;
import com.ljl.agent.ingestion.storage.StoredFile;
import com.ljl.agent.mapper.DocumentChunkMapper;
import com.ljl.agent.mapper.DocumentMapper;
import com.ljl.agent.mapper.KnowledgeBaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultDocumentIngestionServiceTest {

    @TempDir
    Path tempDir;

    private KnowledgeBaseMapper knowledgeBaseMapper;
    private DocumentMapper documentMapper;
    private DocumentChunkMapper documentChunkMapper;
    private DocumentUploadValidator uploadValidator;
    private FileStorageService fileStorageService;
    private ParserRegistry parserRegistry;
    private TextCleaner textCleaner;
    private DocumentIngestionPersistenceService persistenceService;
    private DefaultDocumentIngestionService service;

    @BeforeEach
    void setUp() {
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        documentMapper = mock(DocumentMapper.class);
        documentChunkMapper = mock(DocumentChunkMapper.class);
        uploadValidator = mock(DocumentUploadValidator.class);
        fileStorageService = mock(FileStorageService.class);
        parserRegistry = mock(ParserRegistry.class);
        textCleaner = mock(TextCleaner.class);
        persistenceService = mock(DocumentIngestionPersistenceService.class);
        service = new DefaultDocumentIngestionService(
                knowledgeBaseMapper,
                documentMapper,
                documentChunkMapper,
                uploadValidator,
                fileStorageService,
                parserRegistry,
                textCleaner,
                persistenceService
        );
    }

    @Test
    void shouldRejectForeignKnowledgeBaseBeforeReadingOrWritingFile() {
        KnowledgeBase foreign = knowledgeBase(9L, 99L);
        when(knowledgeBaseMapper.selectById(9L)).thenReturn(foreign);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.upload(7L, 9L, file(), null)
        );

        assertEquals(40302, exception.getCode());
        verifyNoInteractions(uploadValidator, fileStorageService);
        verify(persistenceService, never()).createUploadedDocument(any());
    }

    @Test
    void shouldDeleteStoredFileWhenDatabaseInsertFails() {
        when(knowledgeBaseMapper.selectById(9L))
                .thenReturn(knowledgeBase(9L, 7L));
        ValidatedUpload validated = new ValidatedUpload(
                "guide.txt",
                DocumentFileType.TXT,
                4
        );
        StoredFile stored = new StoredFile(
                "uuid.txt",
                DocumentFileType.TXT,
                4,
                "7/9/uuid.txt",
                "a".repeat(64)
        );
        when(uploadValidator.validate(any())).thenReturn(validated);
        when(fileStorageService.save(
                eq(7L),
                eq(9L),
                any(),
                eq(validated)
        ))
                .thenReturn(stored);
        when(persistenceService.createUploadedDocument(any()))
                .thenThrow(new BusinessException(50008, "数据库失败"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.upload(7L, 9L, file(), null)
        );

        assertEquals(50008, exception.getCode());
        verify(fileStorageService).deleteIfExists("7/9/uuid.txt");
    }

    @Test
    void shouldParseOutsideTransactionAndPersistCleanedContent() {
        Document pending = uploadedDocument(Document.PROCESS_PENDING);
        Document success = uploadedDocument(Document.PROCESS_SUCCESS);
        when(documentMapper.selectById(30L))
                .thenReturn(pending, success);
        when(persistenceService.markParseProcessing(30L, 7L, false))
                .thenReturn(true);
        when(fileStorageService.resolveForRead("7/9/uuid.txt"))
                .thenReturn(tempDir.resolve("uuid.txt"));
        DocumentParser parser = mock(DocumentParser.class);
        when(parserRegistry.requireParser(DocumentFileType.TXT))
                .thenReturn(parser);
        when(parser.parse(any())).thenReturn("\uFEFF正文\r\n");
        when(textCleaner.clean("\uFEFF正文\r\n")).thenReturn("正文");
        when(documentChunkMapper.countByDocumentId(30L)).thenReturn(0L);

        DocumentProcessStatusVO result = service.parse(7L, 30L, false);

        assertEquals(Document.PROCESS_SUCCESS, result.getParseStatus());
        verify(persistenceService).markParseSuccess(30L, 7L, "正文");
        verify(persistenceService, never()).markParseFailed(
                eq(30L),
                eq(7L),
                any()
        );
    }

    @Test
    void shouldMarkFailedAndKeepChunkPendingWhenParserFails() {
        Document pending = uploadedDocument(Document.PROCESS_PENDING);
        when(documentMapper.selectById(30L)).thenReturn(pending);
        when(persistenceService.markParseProcessing(30L, 7L, false))
                .thenReturn(true);
        when(fileStorageService.resolveForRead("7/9/uuid.txt"))
                .thenReturn(tempDir.resolve("uuid.txt"));
        DocumentParser parser = mock(DocumentParser.class);
        when(parserRegistry.requireParser(DocumentFileType.TXT))
                .thenReturn(parser);
        when(parser.parse(any())).thenThrow(
                new DocumentParsingException("文本文件无法按 UTF-8 读取")
        );
        when(persistenceService.markParseFailed(any(), any(), any()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.parse(7L, 30L, false)
        );

        assertEquals(42201, exception.getCode());
        verify(persistenceService).markParseFailed(
                30L,
                7L,
                "文本文件无法按 UTF-8 读取"
        );
        verify(persistenceService, never()).markParseSuccess(
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldRejectForeignStatusQueryBeforeCountingChunks() {
        Document foreign = uploadedDocument(Document.PROCESS_PENDING);
        foreign.setUserId(99L);
        when(documentMapper.selectById(30L)).thenReturn(foreign);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getStatus(7L, 30L)
        );

        assertEquals(40304, exception.getCode());
        verifyNoInteractions(documentChunkMapper);
    }

    private MockMultipartFile file() {
        return new MockMultipartFile(
                "file",
                "guide.txt",
                "text/plain",
                "正文".getBytes(StandardCharsets.UTF_8)
        );
    }

    private KnowledgeBase knowledgeBase(Long id, Long userId) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setUserId(userId);
        return knowledgeBase;
    }

    private Document uploadedDocument(String parseStatus) {
        Document document = new Document();
        document.setId(30L);
        document.setUserId(7L);
        document.setKnowledgeBaseId(9L);
        document.setFileType(DocumentFileType.TXT.name());
        document.setFilePath("7/9/uuid.txt");
        document.setParseStatus(parseStatus);
        document.setChunkStatus(Document.PROCESS_PENDING);
        return document;
    }
}
