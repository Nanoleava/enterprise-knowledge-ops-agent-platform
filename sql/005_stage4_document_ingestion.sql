-- 阶段 4 DAY 1：把 document 从手工正文记录扩展为文件摄取生命周期对象。
-- 本迁移只做向前兼容增量；历史手工文档使用 NOT_APPLICABLE 状态。
-- 可重复执行；若 document 表不存在或已有非法状态，迁移会中止。

DROP PROCEDURE IF EXISTS migrate_005_stage4_document_ingestion;

DELIMITER $$

CREATE PROCEDURE migrate_005_stage4_document_ingestion()
BEGIN
    DECLARE document_table_exists INT DEFAULT 0;
    DECLARE invalid_parse_status_count BIGINT DEFAULT 0;
    DECLARE invalid_chunk_status_count BIGINT DEFAULT 0;

    SELECT COUNT(*)
    INTO document_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'document';

    IF document_table_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '005 migration aborted: document table does not exist';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'original_file_name'
    ) THEN
        ALTER TABLE document
            ADD COLUMN original_file_name VARCHAR(255) NULL
                COMMENT '客户端原始文件名，仅展示' AFTER status;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'stored_file_name'
    ) THEN
        ALTER TABLE document
            ADD COLUMN stored_file_name VARCHAR(255) NULL
                COMMENT '服务端生成的安全文件名' AFTER original_file_name;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'file_type'
    ) THEN
        ALTER TABLE document
            ADD COLUMN file_type VARCHAR(32) NULL
                COMMENT 'TXT/MARKDOWN/PDF' AFTER stored_file_name;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'file_size'
    ) THEN
        ALTER TABLE document
            ADD COLUMN file_size BIGINT NULL
                COMMENT '文件字节数' AFTER file_type;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'file_path'
    ) THEN
        ALTER TABLE document
            ADD COLUMN file_path VARCHAR(500) NULL
                COMMENT '相对文档存储根目录的路径' AFTER file_size;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'file_checksum'
    ) THEN
        ALTER TABLE document
            ADD COLUMN file_checksum CHAR(64) NULL
                COMMENT '文件 SHA-256' AFTER file_path;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'parse_status'
    ) THEN
        ALTER TABLE document
            ADD COLUMN parse_status VARCHAR(32) NOT NULL
                DEFAULT 'NOT_APPLICABLE'
                COMMENT '解析状态' AFTER file_checksum;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'chunk_status'
    ) THEN
        ALTER TABLE document
            ADD COLUMN chunk_status VARCHAR(32) NOT NULL
                DEFAULT 'NOT_APPLICABLE'
                COMMENT '切片状态' AFTER parse_status;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'process_error'
    ) THEN
        ALTER TABLE document
            ADD COLUMN process_error VARCHAR(500) NULL
                COMMENT '可公开的处理失败摘要' AFTER chunk_status;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND column_name = 'processed_at'
    ) THEN
        ALTER TABLE document
            ADD COLUMN processed_at DATETIME NULL
                COMMENT '最近解析或处理成功时间' AFTER process_error;
    END IF;

    UPDATE document
    SET parse_status = 'NOT_APPLICABLE'
    WHERE parse_status IS NULL OR TRIM(parse_status) = '';

    UPDATE document
    SET chunk_status = 'NOT_APPLICABLE'
    WHERE chunk_status IS NULL OR TRIM(chunk_status) = '';

    SELECT COUNT(*)
    INTO invalid_parse_status_count
    FROM document
    WHERE parse_status NOT IN (
        'NOT_APPLICABLE', 'PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'
    );

    SELECT COUNT(*)
    INTO invalid_chunk_status_count
    FROM document
    WHERE chunk_status NOT IN (
        'NOT_APPLICABLE', 'PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'
    );

    IF invalid_parse_status_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '005 migration aborted: invalid document.parse_status exists';
    END IF;

    IF invalid_chunk_status_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '005 migration aborted: invalid document.chunk_status exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'document'
          AND index_name = 'idx_document_user_parse_status'
    ) THEN
        ALTER TABLE document
            ADD KEY idx_document_user_parse_status (user_id, parse_status);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'document'
          AND constraint_name = 'chk_document_file_size_non_negative'
          AND constraint_type = 'CHECK'
    ) THEN
        ALTER TABLE document
            ADD CONSTRAINT chk_document_file_size_non_negative
                CHECK (file_size IS NULL OR file_size >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'document'
          AND constraint_name = 'chk_document_parse_status'
          AND constraint_type = 'CHECK'
    ) THEN
        ALTER TABLE document
            ADD CONSTRAINT chk_document_parse_status
                CHECK (parse_status IN (
                    'NOT_APPLICABLE', 'PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'
                ));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'document'
          AND constraint_name = 'chk_document_chunk_status'
          AND constraint_type = 'CHECK'
    ) THEN
        ALTER TABLE document
            ADD CONSTRAINT chk_document_chunk_status
                CHECK (chunk_status IN (
                    'NOT_APPLICABLE', 'PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'
                ));
    END IF;
END$$

DELIMITER ;

CALL migrate_005_stage4_document_ingestion();
DROP PROCEDURE migrate_005_stage4_document_ingestion;
