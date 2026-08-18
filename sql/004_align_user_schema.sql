-- 阶段 2 Day 5：收敛 user 表与 Java 模型、初始化脚本之间的类型漂移。
-- 目标类型：user.id = BIGINT（Java Long），user.status = TINYINT（0/1，Java Integer）。
-- 使用方式：mysql -u root -p ljl_agent < sql/004_align_user_schema.sql
-- 本脚本可重复执行；遇到超出 BIGINT 范围的 ID 或无法识别的状态值时会中止。

DROP PROCEDURE IF EXISTS migrate_004_align_user_schema;

DELIMITER $$

CREATE PROCEDURE migrate_004_align_user_schema()
BEGIN
    DECLARE user_table_exists INT DEFAULT 0;
    DECLARE status_data_type VARCHAR(64);
    DECLARE invalid_status_count BIGINT DEFAULT 0;
    DECLARE overflowing_id_count BIGINT DEFAULT 0;

    SELECT COUNT(*)
    INTO user_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'user';

    IF user_table_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '004 migration aborted: user table does not exist';
    END IF;

    SELECT data_type
    INTO status_data_type
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND column_name = 'status';

    IF status_data_type IN ('char', 'varchar', 'text', 'tinytext', 'mediumtext') THEN
        UPDATE `user`
        SET status = CASE UPPER(TRIM(CAST(status AS CHAR)))
            WHEN 'ACTIVE' THEN '1'
            WHEN 'ENABLED' THEN '1'
            WHEN 'DISABLED' THEN '0'
            WHEN 'INACTIVE' THEN '0'
            ELSE status
        END;
    END IF;

    SELECT COUNT(*)
    INTO invalid_status_count
    FROM `user`
    WHERE status IS NULL
       OR TRIM(CAST(status AS CHAR)) NOT IN ('0', '1');

    IF invalid_status_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '004 migration aborted: unsupported user.status values exist';
    END IF;

    SELECT COUNT(*)
    INTO overflowing_id_count
    FROM `user`
    WHERE id > 9223372036854775807;

    IF overflowing_id_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '004 migration aborted: user.id exceeds Java Long range';
    END IF;

    ALTER TABLE `user`
        MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
        MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
            COMMENT '用户状态：0-禁用，1-启用';
END$$

DELIMITER ;

CALL migrate_004_align_user_schema();
DROP PROCEDURE migrate_004_align_user_schema;
