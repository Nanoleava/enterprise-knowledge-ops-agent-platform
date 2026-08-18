-- 已存在的 document 表需要通过增量脚本补充同名文档唯一约束。
-- 如果当前已有重复数据，ALTER TABLE 会失败，应先清理重复记录。
SET @document_title_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'document'
      AND index_name = 'uk_document_knowledge_base_title'
);

SET @add_document_title_index_sql = IF(
    @document_title_index_exists = 0,
    'ALTER TABLE document ADD UNIQUE KEY uk_document_knowledge_base_title (knowledge_base_id, title)',
    'SELECT ''uk_document_knowledge_base_title already exists'' AS message'
);

PREPARE add_document_title_index_statement
    FROM @add_document_title_index_sql;
EXECUTE add_document_title_index_statement;
DEALLOCATE PREPARE add_document_title_index_statement;
