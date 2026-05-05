-- ============================
-- 消息幂等性表
-- ============================

-- 消息幂等性记录表
CREATE TABLE IF NOT EXISTS message_idempotent (
    message_id VARCHAR(100) PRIMARY KEY COMMENT '消息唯一ID（主键）',
    queue_name VARCHAR(100) NOT NULL COMMENT '队列名称',
    message_body TEXT COMMENT '消息内容',
    consumer_name VARCHAR(100) COMMENT '消费者名称',
    status TINYINT DEFAULT 1 COMMENT '处理状态：1-处理成功，2-处理失败',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    error_message TEXT COMMENT '错误信息',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_queue_name (queue_name) COMMENT '队列名称索引',
    INDEX idx_created_time (created_time) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息幂等性记录表';
