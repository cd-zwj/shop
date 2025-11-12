-- ========================================
-- 充值模块
-- ========================================

USE `payment_db`;

-- 充值规则表
CREATE TABLE IF NOT EXISTS `recharge_rule` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `recharge_amount` DECIMAL(10,2) NOT NULL COMMENT '充值金额',
  `gift_amount` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '赠送金额',
  `gift_points` INT NOT NULL DEFAULT '0' COMMENT '赠送积分',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `sort_order` INT NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值规则表';

-- 充值订单表
CREATE TABLE IF NOT EXISTS `recharge_order` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '充值订单号',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `recharge_amount` DECIMAL(10,2) NOT NULL COMMENT '充值金额',
  `gift_amount` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '赠送金额',
  `gift_points` INT NOT NULL DEFAULT '0' COMMENT '赠送积分',
  `actual_amount` DECIMAL(10,2) NOT NULL COMMENT '实际到账金额',
  `pay_type` VARCHAR(20) NOT NULL COMMENT '支付方式：WECHAT-微信，ALIPAY-支付宝',
  `pay_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING-待支付，SUCCESS-成功，FAIL-失败',
  `third_party_order_no` VARCHAR(128) DEFAULT NULL COMMENT '第三方订单号',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值订单表';
