-- RAG document module schema.

CREATE TABLE IF NOT EXISTS rag_unit (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    source_id VARCHAR(36) NOT NULL COMMENT '源文件ID',
    file_hash VARCHAR(64) COMMENT '文件SHA-256哈希值',
    user_id VARCHAR(128) NOT NULL COMMENT '所属用户ID',
    filename VARCHAR(512) COMMENT '原始文件名',
    source_type VARCHAR(20) NOT NULL COMMENT '源类型',
    node_type VARCHAR(32) COMMENT '节点类型',
    content TEXT COMMENT '切片内容',
    title VARCHAR(512) COMMENT '标题',
    minio_path VARCHAR(500) COMMENT 'MinIO路径',
    minio_url VARCHAR(1000) COMMENT 'MinIO URL',
    chunk_index INT COMMENT '切片索引',
    parent_id VARCHAR(36) COMMENT '父节点ID',
    tree_level INT COMMENT '树层级',
    ordinal INT COMMENT '同层排序号',
    child_count INT COMMENT '子节点数',
    start_time BIGINT COMMENT '音视频开始时间',
    end_time BIGINT COMMENT '音视频结束时间',
    metadata TEXT COMMENT '元数据JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_rag_unit_source_id (source_id),
    INDEX idx_rag_unit_file_hash (file_hash),
    INDEX idx_rag_unit_user_id (user_id),
    INDEX idx_rag_unit_filename (filename),
    INDEX idx_rag_unit_source_type (source_type),
    INDEX idx_rag_unit_parent_id (parent_id),
    INDEX idx_rag_unit_user_file_hash (user_id, file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG切片存储表';

CREATE TABLE IF NOT EXISTS document_file (
    source_id VARCHAR(36) PRIMARY KEY COMMENT '源文件ID',
    file_hash VARCHAR(64) NOT NULL COMMENT '文件SHA-256哈希值',
    user_id VARCHAR(128) NOT NULL COMMENT '所属用户ID',
    filename VARCHAR(512) NOT NULL COMMENT '原始文件名',
    source_type VARCHAR(20) COMMENT '源类型',
    file_size BIGINT COMMENT '文件大小',
    minio_path VARCHAR(500) COMMENT 'MinIO路径',
    minio_url VARCHAR(1000) COMMENT 'MinIO URL',
    status VARCHAR(32) NOT NULL COMMENT '处理状态',
    error_message TEXT COMMENT '错误信息',
    chunk_count INT DEFAULT 0 COMMENT '叶子分片数',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否已删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_document_file_hash (file_hash),
    INDEX idx_document_user_id (user_id),
    UNIQUE KEY uk_document_user_file_hash (user_id, file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传文档记录表';

CREATE TABLE IF NOT EXISTS ai_feedback (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    message_index INT NOT NULL DEFAULT 0 COMMENT '消息序号',
    user_id VARCHAR(128) NOT NULL COMMENT '用户ID',
    feedback_type VARCHAR(20) NOT NULL COMMENT '反馈类型: UP/DOWN/REGENERATE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ai_feedback_session_id (session_id),
    INDEX idx_ai_feedback_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手反馈记录表';
