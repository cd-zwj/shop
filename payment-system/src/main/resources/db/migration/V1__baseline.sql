-- Flyway baseline migration
-- 创建 auth_user 基础表供后续迁移使用

CREATE TABLE IF NOT EXISTS auth_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  password VARCHAR(255) NOT NULL,
  status INT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 历史迁移脚本见 payment-system/sql/ 目录（00-30 号）。
-- 后续新表结构变更请使用 V2__xxx.sql 命名在此目录下创建。
