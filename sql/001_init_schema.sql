CREATE TABLE IF NOT EXISTS user (
                                    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码摘要',
    email VARCHAR(255) NULL COMMENT '邮箱',
    role VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '用户角色',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0-禁用，1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),

    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='用户表';


CREATE TABLE IF NOT EXISTS knowledge_base (
                                              id BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
                                              user_id BIGINT NOT NULL COMMENT '所属用户ID',
                                              name VARCHAR(100) NOT NULL COMMENT '知识库名称',
    description VARCHAR(500) NULL COMMENT '知识库描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),

    UNIQUE KEY uk_knowledge_base_user_name (user_id, name),

    KEY idx_knowledge_base_user_id (user_id)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='知识库表';

CREATE TABLE IF NOT EXISTS document (
                                        id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
                                        user_id BIGINT NOT NULL COMMENT '所属用户ID',
                                        knowledge_base_id BIGINT NOT NULL COMMENT '所属知识库ID',
                                        title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content LONGTEXT NOT NULL COMMENT '文档内容',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '文档状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),

    KEY idx_document_user_id (user_id),

    KEY idx_document_knowledge_base_id (knowledge_base_id),

    UNIQUE KEY uk_document_knowledge_base_title
        (knowledge_base_id, title)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='文档表';

CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档切片ID',
    document_id BIGINT NOT NULL COMMENT '所属文档ID',
    knowledge_base_id BIGINT NOT NULL COMMENT '所属知识库ID',
    chunk_index INT NOT NULL COMMENT '切片顺序',
    content LONGTEXT NOT NULL COMMENT '切片文本内容',
    metadata MEDIUMTEXT NULL COMMENT '切片附加元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),

    KEY idx_document_chunk_document_id (document_id),

    KEY idx_document_chunk_knowledge_base_id (knowledge_base_id),

    UNIQUE KEY uk_document_chunk_document_index (
        document_id,
        chunk_index
    ),

    CONSTRAINT chk_document_chunk_index_non_negative
        CHECK (chunk_index >= 0),

    CONSTRAINT fk_document_chunk_document
        FOREIGN KEY (document_id) REFERENCES document (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_document_chunk_knowledge_base
        FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base (id)
        ON DELETE CASCADE
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='文档切片表';

CREATE TABLE IF NOT EXISTS chat_session (
                                            id BIGINT NOT NULL AUTO_INCREMENT COMMENT '聊天会话ID',
                                            user_id BIGINT NOT NULL COMMENT '所属用户ID',
                                            title VARCHAR(200) NOT NULL COMMENT '会话标题',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),

    KEY idx_chat_session_user_id (user_id),

    UNIQUE KEY uk_chat_session_id_user (id, user_id)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='聊天会话表';


CREATE TABLE IF NOT EXISTS chat_message (
                                            id BIGINT NOT NULL AUTO_INCREMENT COMMENT '聊天消息ID',
                                            session_id BIGINT NOT NULL COMMENT '所属聊天会话ID',
                                            user_id BIGINT NOT NULL COMMENT '所属用户ID',
                                            role VARCHAR(16) NOT NULL COMMENT '消息角色',
    content LONGTEXT NOT NULL COMMENT '消息内容',
    request_id VARCHAR(64) NOT NULL COMMENT '请求追踪ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),

    KEY idx_chat_message_session_user (session_id, user_id),

    KEY idx_chat_message_user_id (user_id),

    UNIQUE KEY uk_chat_message_request_id (request_id),

    CONSTRAINT chk_chat_message_role
        CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),

    CONSTRAINT fk_chat_message_session_owner
        FOREIGN KEY (session_id, user_id)
        REFERENCES chat_session (id, user_id)
        ON DELETE CASCADE
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='聊天消息表';
