-- 为已有数据库补充聊天表的数据完整性约束。
-- 若历史消息包含不存在的会话，或消息 user_id 与会话所有者不一致，
-- 会话所有者外键创建会失败；应先修复历史数据，避免静默删除业务记录。
-- 当前项目历史库的 user.id 为 BIGINT UNSIGNED，而其他 user_id 为 BIGINT，
-- 因此用户归属由 Service 校验，数据库只约束消息必须属于对应会话所有者。

SET @chat_session_owner_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_session'
      AND index_name = 'uk_chat_session_id_user'
);

SET @add_chat_session_owner_index_sql = IF(
    @chat_session_owner_index_exists = 0,
    'ALTER TABLE chat_session ADD UNIQUE KEY uk_chat_session_id_user (id, user_id)',
    'SELECT ''uk_chat_session_id_user already exists'' AS message'
);

PREPARE add_chat_session_owner_index_statement
    FROM @add_chat_session_owner_index_sql;
EXECUTE add_chat_session_owner_index_statement;
DEALLOCATE PREPARE add_chat_session_owner_index_statement;

SET @chat_message_session_owner_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_message'
      AND index_name = 'idx_chat_message_session_user'
);

SET @add_chat_message_session_owner_index_sql = IF(
    @chat_message_session_owner_index_exists = 0,
    'ALTER TABLE chat_message ADD KEY idx_chat_message_session_user (session_id, user_id)',
    'SELECT ''idx_chat_message_session_user already exists'' AS message'
);

PREPARE add_chat_message_session_owner_index_statement
    FROM @add_chat_message_session_owner_index_sql;
EXECUTE add_chat_message_session_owner_index_statement;
DEALLOCATE PREPARE add_chat_message_session_owner_index_statement;

SET @chat_message_role_check_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'chat_message'
      AND constraint_name = 'chk_chat_message_role'
      AND constraint_type = 'CHECK'
);

SET @add_chat_message_role_check_sql = IF(
    @chat_message_role_check_exists = 0,
    'ALTER TABLE chat_message ADD CONSTRAINT chk_chat_message_role CHECK (role IN (''USER'', ''ASSISTANT'', ''SYSTEM''))',
    'SELECT ''chk_chat_message_role already exists'' AS message'
);

PREPARE add_chat_message_role_check_statement
    FROM @add_chat_message_role_check_sql;
EXECUTE add_chat_message_role_check_statement;
DEALLOCATE PREPARE add_chat_message_role_check_statement;

SET @chat_message_session_owner_fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'chat_message'
      AND constraint_name = 'fk_chat_message_session_owner'
      AND constraint_type = 'FOREIGN KEY'
);

SET @add_chat_message_session_owner_fk_sql = IF(
    @chat_message_session_owner_fk_exists = 0,
    'ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_session_owner FOREIGN KEY (session_id, user_id) REFERENCES chat_session (id, user_id) ON DELETE CASCADE',
    'SELECT ''fk_chat_message_session_owner already exists'' AS message'
);

PREPARE add_chat_message_session_owner_fk_statement
    FROM @add_chat_message_session_owner_fk_sql;
EXECUTE add_chat_message_session_owner_fk_statement;
DEALLOCATE PREPARE add_chat_message_session_owner_fk_statement;
