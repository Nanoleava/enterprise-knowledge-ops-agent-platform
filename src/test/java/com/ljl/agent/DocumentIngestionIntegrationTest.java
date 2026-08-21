package com.ljl.agent;

import com.ljl.agent.dto.response.DocumentProcessStatusVO;
import com.ljl.agent.dto.response.DocumentUploadResponse;
import com.ljl.agent.entity.Document;
import com.ljl.agent.entity.KnowledgeBase;
import com.ljl.agent.entity.User;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.ingestion.DocumentIngestionService;
import com.ljl.agent.ingestion.storage.FileStorageService;
import com.ljl.agent.mapper.DocumentMapper;
import com.ljl.agent.mapper.KnowledgeBaseMapper;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.util.PasswordUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(
        named = "DB_PASSWORD",
        matches = ".+",
        disabledReason = "未提供 DB_PASSWORD，跳过真实 MySQL 文档摄取测试"
)
class DocumentIngestionIntegrationTest extends AbstractIntegrationTest {

    private static final Path STORAGE_ROOT = createStorageRoot();

    @DynamicPropertySource
    static void ingestionProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "app.document.storage-root",
                () -> STORAGE_ROOT.toString()
        );
    }

    @Autowired
    private DocumentIngestionService ingestionService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> documentIds = new ArrayList<>();
    private final List<Long> knowledgeBaseIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    private User owner;
    private KnowledgeBase knowledgeBase;

    @BeforeEach
    void setUpData() {
        String marker = String.valueOf(System.nanoTime());
        owner = insertUser("ingestion_owner_" + marker);
        knowledgeBase = insertKnowledgeBase(
                owner.getId(),
                "摄取集成测试_" + marker
        );
    }

    @AfterEach
    void cleanUpDataAndFiles() {
        for (Long documentId : documentIds) {
            Document document = documentMapper.selectById(documentId);
            if (document != null && document.getFilePath() != null) {
                try {
                    fileStorageService.deleteIfExists(document.getFilePath());
                } catch (RuntimeException ignored) {
                    // 精确测试目录会在 @AfterAll 再清理一次。
                }
            }
            jdbcTemplate.update("DELETE FROM document WHERE id = ?", documentId);
        }
        for (Long knowledgeBaseId : knowledgeBaseIds.reversed()) {
            jdbcTemplate.update(
                    "DELETE FROM knowledge_base WHERE id = ?",
                    knowledgeBaseId
            );
        }
        for (Long userId : userIds.reversed()) {
            jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", userId);
        }
    }

    @AfterAll
    static void cleanStorageRoot() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }
        try (var paths = Files.walk(STORAGE_ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void shouldUploadAndParseTxtAndMarkdownIntoDocumentContent() {
        DocumentUploadResponse txt = upload(
                "guide.txt",
                "text/plain",
                "\uFEFF第一行\r\n\r\n\r\n第二行  ",
                "TXT 指南"
        );
        DocumentUploadResponse markdown = upload(
                "readme.md",
                "text/markdown",
                "# 标题\r\n\r\n- 列表\r\n\r\n```java\r\nint n = 1;\r\n```",
                null
        );

        Document txtPending = documentMapper.selectById(txt.getDocumentId());
        Document mdPending = documentMapper.selectById(markdown.getDocumentId());
        assertEquals("", txtPending.getContent());
        assertEquals(Document.PROCESS_PENDING, txtPending.getParseStatus());
        assertEquals(Document.PROCESS_PENDING, txtPending.getChunkStatus());
        assertEquals("guide.txt", txtPending.getOriginalFileName());
        assertNotEquals("guide.txt", txtPending.getStoredFileName());
        assertFalse(Path.of(txtPending.getFilePath()).isAbsolute());
        assertEquals(64, txtPending.getFileChecksum().length());
        assertTrue(Files.isRegularFile(
                fileStorageService.resolveForRead(txtPending.getFilePath())
        ));
        assertTrue(Files.isRegularFile(
                fileStorageService.resolveForRead(mdPending.getFilePath())
        ));

        DocumentProcessStatusVO txtStatus = ingestionService.parse(
                owner.getId(),
                txt.getDocumentId(),
                false
        );
        DocumentProcessStatusVO mdStatus = ingestionService.parse(
                owner.getId(),
                markdown.getDocumentId(),
                false
        );

        assertEquals(Document.PROCESS_SUCCESS, txtStatus.getParseStatus());
        assertEquals(Document.PROCESS_PENDING, txtStatus.getChunkStatus());
        assertEquals(0, txtStatus.getChunkCount());
        assertEquals(
                "第一行\n\n\n第二行",
                documentMapper.selectById(txt.getDocumentId()).getContent()
        );
        Document parsedMarkdown = documentMapper.selectById(
                markdown.getDocumentId()
        );
        assertEquals(Document.PROCESS_SUCCESS, mdStatus.getParseStatus());
        assertTrue(parsedMarkdown.getContent().startsWith("# 标题\n\n- 列表"));
        assertTrue(parsedMarkdown.getContent().contains("```java"));
        assertNotNull(parsedMarkdown.getProcessedAt());
    }

    @Test
    void shouldPersistFailedStatusForWhitespaceDocumentAndAllowRetry() {
        DocumentUploadResponse response = upload(
                "blank.txt",
                "text/plain",
                " \r\n\t\r\n ",
                "空白文档"
        );

        BusinessException firstFailure = assertThrows(
                BusinessException.class,
                () -> ingestionService.parse(
                        owner.getId(),
                        response.getDocumentId(),
                        false
                )
        );
        assertEquals(42201, firstFailure.getCode());

        DocumentProcessStatusVO failed = ingestionService.getStatus(
                owner.getId(),
                response.getDocumentId()
        );
        assertEquals(Document.PROCESS_FAILED, failed.getParseStatus());
        assertEquals(Document.PROCESS_PENDING, failed.getChunkStatus());
        assertEquals("文档解析后没有可用文本", failed.getErrorMessage());
        assertEquals(
                "",
                documentMapper.selectById(response.getDocumentId()).getContent()
        );
        assertTrue(Files.isRegularFile(fileStorageService.resolveForRead(
                documentMapper.selectById(response.getDocumentId()).getFilePath()
        )));

        BusinessException retryFailure = assertThrows(
                BusinessException.class,
                () -> ingestionService.parse(
                        owner.getId(),
                        response.getDocumentId(),
                        false
                )
        );
        assertEquals(42201, retryFailure.getCode());
    }

    @Test
    void shouldRejectForeignKnowledgeBaseWithoutDatabaseOrFilePollution()
            throws IOException {
        User other = insertUser("ingestion_other_" + System.nanoTime());
        long documentCountBefore = documentCount();
        long fileCountBefore = storedFileCount();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> ingestionService.upload(
                        other.getId(),
                        knowledgeBase.getId(),
                        textFile("foreign.txt", "不可写入"),
                        "越权上传"
                )
        );

        assertEquals(40302, exception.getCode());
        assertEquals(documentCountBefore, documentCount());
        assertEquals(fileCountBefore, storedFileCount());
    }

    private DocumentUploadResponse upload(
            String fileName,
            String contentType,
            String content,
            String title
    ) {
        DocumentUploadResponse response = ingestionService.upload(
                owner.getId(),
                knowledgeBase.getId(),
                new MockMultipartFile(
                        "file",
                        fileName,
                        contentType,
                        content.getBytes(StandardCharsets.UTF_8)
                ),
                title
        );
        documentIds.add(response.getDocumentId());
        return response;
    }

    private MockMultipartFile textFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private User insertUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtils.hash("Test@123456"));
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_ENABLED);
        assertEquals(1, userMapper.insert(user));
        userIds.add(user.getId());
        return user;
    }

    private KnowledgeBase insertKnowledgeBase(Long userId, String name) {
        KnowledgeBase result = new KnowledgeBase();
        result.setUserId(userId);
        result.setName(name);
        assertEquals(1, knowledgeBaseMapper.insert(result));
        knowledgeBaseIds.add(result.getId());
        return result;
    }

    private long documentCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM document",
                Long.class
        );
        return count == null ? 0 : count;
    }

    private long storedFileCount() throws IOException {
        try (var paths = Files.walk(STORAGE_ROOT)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("ljl-stage4-day1-")
                    .toAbsolutePath()
                    .normalize();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
