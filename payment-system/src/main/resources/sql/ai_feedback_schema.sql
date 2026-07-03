-- AI 反馈记录表
CREATE TABLE IF NOT EXISTS ai_feedback (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    message_index INT NOT NULL DEFAULT 0 COMMENT '消息序号（会话内第几条）',
    user_id VARCHAR(128) NOT NULL COMMENT '用户ID',
    feedback_type VARCHAR(20) NOT NULL COMMENT '反馈类型：UP/DOWN/REGENERATE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手反馈记录表';
