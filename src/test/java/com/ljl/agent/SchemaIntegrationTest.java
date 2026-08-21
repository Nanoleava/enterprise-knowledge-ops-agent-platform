package com.ljl.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(
        named = "DB_PASSWORD",
        matches = ".+",
        disabledReason = "未提供 DB_PASSWORD，跳过真实 MySQL schema 测试"
)
class SchemaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldMatchStageFourDayOneCanonicalSchema() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'user', 'knowledge_base', 'document',
                      'document_chunk', 'chat_session', 'chat_message'
                  )
                ORDER BY table_name
                """,
                String.class
        );

        assertEquals(List.of(
                "chat_message",
                "chat_session",
                "document",
                "document_chunk",
                "knowledge_base",
                "user"
        ), tables);
        assertEquals("bigint", columnType("user", "id"));
        assertEquals("tinyint", columnType("user", "status"));
        assertEquals("bigint", columnType("knowledge_base", "user_id"));
        assertEquals("bigint", columnType("document", "user_id"));
        assertEquals("bigint", columnType("chat_session", "user_id"));
        assertEquals("bigint", columnType("chat_message", "user_id"));
        assertEquals("varchar", columnType("document", "original_file_name"));
        assertEquals("varchar", columnType("document", "stored_file_name"));
        assertEquals("varchar", columnType("document", "file_type"));
        assertEquals("bigint", columnType("document", "file_size"));
        assertEquals("varchar", columnType("document", "file_path"));
        assertEquals("char", columnType("document", "file_checksum"));
        assertEquals("varchar", columnType("document", "parse_status"));
        assertEquals("varchar", columnType("document", "chunk_status"));
        assertEquals("varchar", columnType("document", "process_error"));
        assertEquals("datetime", columnType("document", "processed_at"));

        assertEquals(1, indexCount("user", "uk_user_username"));
        assertEquals(1, indexCount(
                "knowledge_base",
                "uk_knowledge_base_user_name"
        ));
        assertEquals(1, indexCount(
                "document",
                "uk_document_knowledge_base_title"
        ));
        assertEquals(1, indexCount(
                "document_chunk",
                "uk_document_chunk_document_index"
        ));
        assertEquals(1, indexCount(
                "chat_message",
                "uk_chat_message_request_id"
        ));
        assertEquals(1, indexCount(
                "document",
                "idx_document_user_parse_status"
        ));
        assertEquals("NOT_APPLICABLE", columnDefault(
                "document",
                "parse_status"
        ));
        assertEquals("NOT_APPLICABLE", columnDefault(
                "document",
                "chunk_status"
        ));
    }

    private String columnType(String table, String column) {
        return jdbcTemplate.queryForObject(
                """
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                String.class,
                table,
                column
        );
    }

    private int indexCount(String table, String index) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """,
                Integer.class,
                table,
                index
        );
        return count == null ? 0 : count;
    }

    private String columnDefault(String table, String column) {
        return jdbcTemplate.queryForObject(
                """
                SELECT column_default
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                String.class,
                table,
                column
        );
    }
}
