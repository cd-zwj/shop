-- ========================================
-- 用户积分模块
-- ========================================

USE `payment_db`;

-- 用户积分表
CREATE TABLE IF NOT EXISTS `user_points` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `points` INT NOT NULL DEFAULT '0' COMMENT '当前积分',
  `total_earned` INT NOT NULL DEFAULT '0' COMMENT '累计获得积分',
  `total_used` INT NOT NULL DEFAULT '0' COMMENT '累计使用积分',
  `level` INT NOT NULL DEFAULT '1' COMMENT '会员等级',
  `version` INT NOT NULL DEFAULT '0' COMMENT '版本号（乐观锁）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分表';

-- 积分变动日志表
CREATE TABLE IF NOT EXISTS `points_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `change_type` VARCHAR(20) NOT NULL COMMENT '变动类型：EARN-获得，USE-使用，EXPIRE-过期，EXCHANGE-兑换',
  `change_points` INT NOT NULL COMMENT '变动积分',
  `points_before` INT NOT NULL COMMENT '变动前积分',
  `points_after` INT NOT NULL COMMENT '变动后积分',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动日志表';

-- 积分规则表
CREATE TABLE IF NOT EXISTS `points_rule` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `rule_type` VARCHAR(20) NOT NULL COMMENT '规则类型：PAYMENT-支付获得，SIGNIN-签到，SHARE-分享',
  `points_amount` INT NOT NULL COMMENT '积分数量',
  `condition_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '条件金额（支付满多少）',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_rule_type` (`rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分规则表';
