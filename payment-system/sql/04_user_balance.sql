-- ========================================
-- 用户余额模块
-- ========================================

USE `payment_db`;

-- 用户余额表
CREATE TABLE IF NOT EXISTS `user_balance` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `balance` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额',
  `frozen_balance` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '冻结余额',
  `total_recharge` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '累计充值',
  `total_consume` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '累计消费',
  `version` INT NOT NULL DEFAULT '0' COMMENT '版本号（乐观锁）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户余额表';

-- 余额变动日志表
CREATE TABLE IF NOT EXISTS `balance_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `change_type` VARCHAR(20) NOT NULL COMMENT '变动类型：RECHARGE-充值，CONSUME-消费，REFUND-退款，WITHDRAW-提现',
  `change_amount` DECIMAL(10,2) NOT NULL COMMENT '变动金额',
  `balance_before` DECIMAL(10,2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(10,2) NOT NULL COMMENT '变动后余额',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额变动日志表';
