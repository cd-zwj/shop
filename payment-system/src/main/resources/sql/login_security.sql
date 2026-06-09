CREATE TABLE IF NOT EXISTS login_fail_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_type VARCHAR(20) NOT NULL,
    account_value VARCHAR(128) NOT NULL,
    fail_count INT NOT NULL DEFAULT 0,
    last_fail_time DATETIME,
    lock_start_time DATETIME,
    lock_end_time DATETIME,
    lock_status VARCHAR(20) NOT NULL DEFAULT 'UNLOCKED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_login_fail_account (account_type, account_value),
    KEY idx_login_fail_lock_status (lock_status, lock_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录失败记录表';
