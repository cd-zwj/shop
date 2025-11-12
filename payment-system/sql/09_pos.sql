-- ========================================
-- POS 收银模块
-- ========================================

USE `payment_db`;

-- POS 会话表
CREATE TABLE IF NOT EXISTS `pos_session` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `device_id` VARCHAR(64) DEFAULT NULL COMMENT '设备ID',
  `cashier_id` BIGINT(20) DEFAULT NULL COMMENT '收银员ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-活跃，CLOSED-已关闭',
  `cart_data` JSON DEFAULT NULL COMMENT '购物车数据',
  `total_amount` DECIMAL(10,2) DEFAULT '0.00' COMMENT '总金额',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='POS会话表';

-- 扫码记录表
CREATE TABLE IF NOT EXISTS `scan_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `device_id` VARCHAR(64) DEFAULT NULL COMMENT '设备ID',
  `product_code` VARCHAR(64) NOT NULL COMMENT '商品编码',
  `product_id` BIGINT(20) DEFAULT NULL COMMENT '商品ID',
  `scan_status` VARCHAR(20) NOT NULL COMMENT '扫码状态：SUCCESS-成功，NOT_FOUND-商品不存在，ERROR-错误',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_product_code` (`product_code`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码记录表';
