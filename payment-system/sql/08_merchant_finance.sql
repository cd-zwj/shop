-- ========================================
-- 商家财务模块
-- ========================================

USE `payment_db`;

-- 商家余额表
CREATE TABLE IF NOT EXISTS `merchant_balance` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `balance` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额',
  `frozen_balance` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '冻结余额',
  `total_income` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '累计收入',
  `total_withdrawal` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '累计提现',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家余额表';

-- 提现申请表
CREATE TABLE IF NOT EXISTS `withdrawal` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '提现金额',
  `bank_name` VARCHAR(100) DEFAULT NULL COMMENT '银行名称',
  `bank_account` VARCHAR(50) DEFAULT NULL COMMENT '银行账号',
  `account_name` VARCHAR(100) DEFAULT NULL COMMENT '账户名称',
  `status` INT NOT NULL DEFAULT '0' COMMENT '状态：0-待审核，1-已通过，2-已拒绝',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
  `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `approver_id` BIGINT(20) DEFAULT NULL COMMENT '审核人ID',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';
