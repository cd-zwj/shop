-- ========================================
-- 退款模块
-- 基于 v1 统一支付单模型扩展
-- ========================================

USE `payment_db`;

-- 退款业务单表
CREATE TABLE IF NOT EXISTS `refund_order` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
  `biz_type` VARCHAR(20) NOT NULL COMMENT '业务类型：SALES_ORDER-消费订单，RECHARGE_ORDER-充值订单',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
  `payment_bill_no` VARCHAR(64) DEFAULT NULL COMMENT '支付单号',
  `channel_code` VARCHAR(32) DEFAULT NULL COMMENT '支付渠道',
  `refund_reason` VARCHAR(255) DEFAULT NULL COMMENT '退款原因',
  `apply_amount` DECIMAL(18,2) NOT NULL COMMENT '申请退款金额',
  `refund_amount` DECIMAL(18,2) NOT NULL COMMENT '实际退款金额',
  `wallet_refund_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '钱包退款金额',
  `external_refund_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '外部渠道退款金额',
  `refund_status` VARCHAR(20) NOT NULL DEFAULT 'APPLIED' COMMENT '退款状态：APPLIED-已申请，PROCESSING-处理中，SUCCESS-成功，FAIL-失败，CLOSED-关闭',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING-待审核，APPROVED-已通过，REJECTED-已拒绝',
  `audit_by` BIGINT(20) DEFAULT NULL COMMENT '审核人ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `success_time` DATETIME DEFAULT NULL COMMENT '退款成功时间',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_refund_biz` (`biz_type`, `biz_no`),
  KEY `idx_refund_tenant_status` (`tenant_id`, `refund_status`),
  KEY `idx_refund_user_time` (`platform_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款业务单表';

-- 退款流水表
CREATE TABLE IF NOT EXISTS `refund_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
  `payment_bill_no` VARCHAR(64) DEFAULT NULL COMMENT '支付单号',
  `channel_code` VARCHAR(32) NOT NULL COMMENT '支付渠道',
  `refund_amount` DECIMAL(18,2) NOT NULL COMMENT '退款金额',
  `third_party_bill_no` VARCHAR(128) DEFAULT NULL COMMENT '原第三方支付单号',
  `third_party_refund_no` VARCHAR(128) DEFAULT NULL COMMENT '第三方退款单号',
  `channel_status` VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT '渠道状态：PROCESSING-处理中，SUCCESS-成功，FAIL-失败',
  `notify_data` TEXT COMMENT '通知报文',
  `request_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `notify_time` DATETIME DEFAULT NULL COMMENT '通知时间',
  `success_time` DATETIME DEFAULT NULL COMMENT '成功时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_channel_no` (`channel_code`, `refund_no`),
  KEY `idx_refund_record_bill` (`payment_bill_no`),
  KEY `idx_refund_record_status` (`channel_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款流水表';

-- 退款回调记录表
CREATE TABLE IF NOT EXISTS `refund_callback_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
  `channel_code` VARCHAR(32) NOT NULL COMMENT '渠道编码',
  `callback_request_id` VARCHAR(128) NOT NULL COMMENT '回调请求ID',
  `callback_body` TEXT COMMENT '回调报文',
  `verify_status` VARCHAR(20) NOT NULL COMMENT '验签状态：SUCCESS-成功，FAIL-失败',
  `process_status` VARCHAR(20) NOT NULL COMMENT '处理状态：SUCCESS-成功，FAIL-失败，IGNORED-忽略',
  `callback_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回调时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_callback_request` (`channel_code`, `callback_request_id`),
  KEY `idx_refund_callback_no` (`refund_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款回调记录表';

-- 退款补查任务表
CREATE TABLE IF NOT EXISTS `refund_reconcile_task` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_no` VARCHAR(64) NOT NULL COMMENT '任务编号',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
  `channel_code` VARCHAR(32) NOT NULL COMMENT '支付渠道',
  `task_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING-待执行，PROCESSING-处理中，SUCCESS-成功，FAIL-失败，CANCELLED-取消',
  `retry_count` INT NOT NULL DEFAULT '0' COMMENT '重试次数',
  `max_retry_count` INT NOT NULL DEFAULT '10' COMMENT '最大重试次数',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
  `last_result` VARCHAR(255) DEFAULT NULL COMMENT '最后一次处理结果',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_reconcile_task_no` (`task_no`),
  KEY `idx_refund_reconcile_status` (`task_status`, `next_retry_time`),
  KEY `idx_refund_reconcile_refund_no` (`refund_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款补查任务表';
