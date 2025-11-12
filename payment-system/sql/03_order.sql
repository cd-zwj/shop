-- ========================================
-- 订单模块
-- ========================================

USE `payment_db`;

-- 订单表
CREATE TABLE IF NOT EXISTS `payment_order` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
  `pay_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '实付金额',
  `pay_type` VARCHAR(20) NOT NULL COMMENT '支付方式：WECHAT-微信，ALIPAY-支付宝',
  `order_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING-待支付，PAID-已支付，CANCELLED-已取消，REFUNDED-已退款',
  `pay_status` VARCHAR(20) DEFAULT NULL COMMENT '支付状态：SUCCESS-成功，FAIL-失败',
  `third_party_order_no` VARCHAR(128) DEFAULT NULL COMMENT '第三方订单号',
  `subject` VARCHAR(200) NOT NULL COMMENT '订单标题',
  `body` VARCHAR(500) DEFAULT NULL COMMENT '订单描述',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '订单过期时间',
  `notify_url` VARCHAR(255) DEFAULT NULL COMMENT '回调地址',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单商品明细表
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `order_id` BIGINT(20) NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `product_id` BIGINT(20) NOT NULL COMMENT '商品ID',
  `product_code` VARCHAR(64) NOT NULL COMMENT '商品编码',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `product_image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `quantity` INT NOT NULL COMMENT '数量',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品明细表';

-- 支付记录表
CREATE TABLE IF NOT EXISTS `payment_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `order_id` BIGINT(20) NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `pay_type` VARCHAR(20) NOT NULL COMMENT '支付方式：WECHAT-微信，ALIPAY-支付宝',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `third_party_order_no` VARCHAR(128) DEFAULT NULL COMMENT '第三方订单号',
  `transaction_id` VARCHAR(128) DEFAULT NULL COMMENT '交易流水号',
  `pay_status` VARCHAR(20) NOT NULL COMMENT '支付状态：SUCCESS-成功，FAIL-失败，PROCESSING-处理中',
  `notify_data` TEXT COMMENT '回调通知数据',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `notify_time` DATETIME DEFAULT NULL COMMENT '通知时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';
