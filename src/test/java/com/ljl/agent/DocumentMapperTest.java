package com.ljl.agent;

import com.ljl.agent.entity.Document;
import com.ljl.agent.entity.DocumentChunk;
import com.ljl.agent.entity.KnowledgeBase;
import com.ljl.agent.entity.User;
import com.ljl.agent.mapper.DocumentChunkMapper;
import com.ljl.agent.mapper.DocumentMapper;
import com.ljl.agent.mapper.KnowledgeBaseMapper;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.util.PasswordUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@Rollback
@EnabledIfEnvironmentVariable(
        named = "DB_PASSWORD",
        matches = ".+",
        disabledReason = "未提供 DB_PASSWORD，跳过真实 MySQL 集成测试"
)
class DocumentMapperTest extends AbstractIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Test
    void shouldRunDynamicPageSqlAndCascadeDeleteChunks() {
        String marker = String.valueOf(System.nanoTime());
        User user = insertUser("document_mapper_" + marker);
        KnowledgeBase target = insertKnowledgeBase(
                user.getId(),
                "目标知识库_" + marker
        );
        KnowledgeBase other = insertKnowledgeBase(
                user.getId(),
                "其他知识库_" + marker
        );

        Document firstMatch = insertDocument(
                user.getId(),
                target.getId(),
                "Java 分页_" + marker,
                "正文"
        );
        Document secondMatch = insertDocument(
                user.getId(),
                target.getId(),
                "普通标题_" + marker,
                "内容包含 Java"
        );
        insertDocument(
                user.getId(),
                target.getId(),
                "不匹配_" + marker,
                "MyBatis"
        );
        insertDocument(
                user.getId(),
                other.getId(),
                "Java 但属于其他知识库_" + marker,
                "Java"
        );

        assertEquals(2L, documentMapper.countPage(
                user.getId(),
                "Java",
                target.getId()
        ));
        List<Document> page = documentMapper.selectPage(
                user.getId(),
                "Java",
                target.getId(),
                0,
                10
        );
        assertEquals(2, page.size());
        assertEquals(secondMatch.getId(), page.getFirst().getId());
        assertEquals(firstMatch.getId(), page.getLast().getId());

        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(firstMatch.getId());
        chunk.setKnowledgeBaseId(target.getId());
        chunk.setChunkIndex(0);
        chunk.setContent("待级联删除的切片");
        assertEquals(1, documentChunkMapper.insert(chunk));
        assertEquals(
                1,
                documentChunkMapper
                        .selectByDocumentId(firstMatch.getId())
                        .size()
        );

        assertEquals(1, documentMapper.deleteByIdAndUserId(
                firstMatch.getId(),
                user.getId()
        ));
        assertNull(documentMapper.selectById(firstMatch.getId()));
        assertEquals(
                List.of(),
                documentChunkMapper.selectByDocumentId(firstMatch.getId())
        );
    }

    @Test
    void shouldPersistUploadedMetadataAndMoveParseStateConditionally() {
        String marker = String.valueOf(System.nanoTime());
        User user = insertUser("document_ingestion_" + marker);
        KnowledgeBase knowledgeBase = insertKnowledgeBase(
                user.getId(),
                "摄取知识库_" + marker
        );

        Document document = new Document();
        document.setUserId(user.getId());
        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setTitle("guide_" + marker);
        document.setContent("");
        document.setStatus(Document.STATUS_ACTIVE);
        document.setOriginalFileName("guide.txt");
        document.setStoredFileName("a".repeat(32) + ".txt");
        document.setFileType("TXT");
        document.setFileSize(6L);
        document.setFilePath(
                user.getId() + "/" + knowledgeBase.getId() + "/a.txt"
        );
        document.setFileChecksum("b".repeat(64));
        document.setParseStatus(Document.PROCESS_PENDING);
        document.setChunkStatus(Document.PROCESS_PENDING);

        assertEquals(1, documentMapper.insertUploaded(document));
        Document pending = documentMapper.selectById(document.getId());
        assertNotNull(pending);
        assertEquals("guide.txt", pending.getOriginalFileName());
        assertEquals("TXT", pending.getFileType());
        assertEquals(Document.PROCESS_PENDING, pending.getParseStatus());
        assertEquals(Document.PROCESS_PENDING, pending.getChunkStatus());

        assertEquals(1, documentMapper.markParseProcessing(
                document.getId(),
                user.getId(),
                false
        ));
        assertEquals(0, documentMapper.markParseProcessing(
                document.getId(),
                user.getId(),
                false
        ));
        assertEquals(1, documentMapper.updateParseSuccess(
                document.getId(),
                user.getId(),
                "解析正文"
        ));

        Document success = documentMapper.selectById(document.getId());
        assertEquals("解析正文", success.getContent());
        assertEquals(Document.PROCESS_SUCCESS, success.getParseStatus());
        assertNull(success.getProcessError());
        assertNotNull(success.getProcessedAt());
        assertEquals(0L, documentChunkMapper.countByDocumentId(document.getId()));
    }

    private User insertUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtils.hash("Test@123456"));
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_ENABLED);
        assertEquals(1, userMapper.insert(user));
        return user;
    }

    private KnowledgeBase insertKnowledgeBase(Long userId, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(name);
        assertEquals(1, knowledgeBaseMapper.insert(knowledgeBase));
        return knowledgeBase;
    }

    private Document insertDocument(
            Long userId,
            Long knowledgeBaseId,
            String title,
            String content
    ) {
        Document document = new Document();
        document.setUserId(userId);
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(title);
        document.setContent(content);
        document.setStatus(Document.STATUS_ACTIVE);
        assertEquals(1, documentMapper.insert(document));
        return document;
    }
}
