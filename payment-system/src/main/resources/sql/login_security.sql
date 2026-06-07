CREATE TABLE IF NOT EXISTS login_fail_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account VARCHAR(100) NOT NULL,
    ip VARCHAR(50),
    fail_count INT DEFAULT 0,
    last_fail_time DATETIME,
    locked_until DATETIME,
    tenant_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_account (account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录失败记录表';
