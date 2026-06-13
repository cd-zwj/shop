-- ========================================
-- merchant_balance 添加乐观锁 version 列
-- ========================================

USE `payment_db`;

ALTER TABLE `merchant_balance`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `deleted`;
