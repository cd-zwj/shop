-- ========================================
-- merchant_balance 添加乐观锁 version 列
-- ========================================

USE `payment_db`;

SET @merchant_balance_version_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'merchant_balance'
      AND column_name = 'version'
);
SET @merchant_balance_version_sql = IF(
    @merchant_balance_version_exists = 0,
    'ALTER TABLE `merchant_balance` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `deleted`',
    'SELECT 1'
);
PREPARE merchant_balance_version_stmt FROM @merchant_balance_version_sql;
EXECUTE merchant_balance_version_stmt;
DEALLOCATE PREPARE merchant_balance_version_stmt;
