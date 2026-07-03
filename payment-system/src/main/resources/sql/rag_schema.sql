-- RAG Knowledge Base Schema (Combined V1 + V2 + V3)

CREATE DATABASE IF NOT EXISTS rag_knowledge DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rag_knowledge;

CREATE TABLE IF NOT EXISTS rag_unit (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    source_id VARCHAR(36) NOT NULL COMMENT '源文件ID',
    file_hash VARCHAR(64) COMMENT '文件SHA-256哈希值（用于去重）',
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

    -- 索引优化
    INDEX idx_source_id (source_id),
    INDEX idx_file_hash (file_hash),
    INDEX idx_user_id (user_id),
    INDEX idx_filename (filename),
    INDEX idx_source_type (source_type),
    INDEX idx_parent_id (parent_id),
    INDEX idx_user_file_hash (user_id, file_hash)
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

CREATE TABLE IF NOT EXISTS auth_user (
    id VARCHAR(36) PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    email VARCHAR(255) COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_auth_user_username (username),
    UNIQUE KEY uk_auth_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认证用户表';

CREATE TABLE IF NOT EXISTS auth_role (
    id VARCHAR(36) PRIMARY KEY COMMENT '角色ID',
    code VARCHAR(64) NOT NULL COMMENT '角色编码',
    name VARCHAR(128) NOT NULL COMMENT '角色名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_auth_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS auth_permission (
    id VARCHAR(36) PRIMARY KEY COMMENT '权限ID',
    code VARCHAR(128) NOT NULL COMMENT '权限编码',
    name VARCHAR(255) NOT NULL COMMENT '权限名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_auth_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS auth_user_role (
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    role_id VARCHAR(36) NOT NULL COMMENT '角色ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id, role_id),
    INDEX idx_auth_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS auth_role_permission (
    role_id VARCHAR(36) NOT NULL COMMENT '角色ID',
    permission_id VARCHAR(36) NOT NULL COMMENT '权限ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, permission_id),
    INDEX idx_auth_role_permission_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';
