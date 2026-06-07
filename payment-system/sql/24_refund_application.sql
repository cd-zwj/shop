-- ========================================
-- 售后退款申请模块
-- 用户发起退款/退货退款申请，商户审核
-- ========================================

USE `payment_db`;

CREATE TABLE IF NOT EXISTS `refund_application` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款申请单号',
  `order_no` VARCHAR(64) NOT NULL COMMENT '关联订单号',
  `order_item_id` BIGINT(20) DEFAULT NULL COMMENT '关联订单项ID，部分退款时使用',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '申请用户ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `refund_type` VARCHAR(20) NOT NULL COMMENT '退款类型：REFUND_ONLY-仅退款，RETURN_REFUND-退货退款',
  `refund_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '退款状态：PENDING-待审核，APPROVED-已同意，REJECTED-已拒绝，PROCESSING-处理中，COMPLETED-已完成，CANCELLED-已取消',
  `refund_amount` DECIMAL(18,2) NOT NULL COMMENT '退款金额',
  `reason` VARCHAR(200) NOT NULL COMMENT '退款原因',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '详细描述',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
  `admin_id` BIGINT(20) DEFAULT NULL COMMENT '审核人ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_application_no` (`refund_no`),
  KEY `idx_refund_application_order` (`order_no`),
  KEY `idx_refund_application_tenant_status` (`tenant_id`, `refund_status`),
  KEY `idx_refund_application_user_time` (`platform_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后退款申请表';
