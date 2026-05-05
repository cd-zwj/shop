-- ========================================
-- 消息重试与消费日志模块
-- 补充统一消息治理能力
-- ========================================

USE `payment_db`;

-- 消息消费日志表
CREATE TABLE IF NOT EXISTS `message_consume_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID',
  `queue_name` VARCHAR(64) NOT NULL COMMENT '队列名称',
  `consumer_name` VARCHAR(100) NOT NULL COMMENT '消费者名称',
  `biz_type` VARCHAR(32) DEFAULT NULL COMMENT '业务类型',
  `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
  `consume_status` VARCHAR(20) NOT NULL COMMENT '消费状态：SUCCESS-成功，FAIL-失败，IGNORED-忽略',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  `consume_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_consume_message` (`message_id`, `consume_time`),
  KEY `idx_message_consume_queue` (`queue_name`, `consume_status`),
  KEY `idx_message_consume_biz` (`biz_type`, `biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息消费日志表';

-- 重试任务表
CREATE TABLE IF NOT EXISTS `retry_task` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_no` VARCHAR(64) NOT NULL COMMENT '任务编号',
  `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型：PAYMENT_CALLBACK-支付回调，ORDER_CLOSE-订单关闭，RECHARGE_CREDIT-充值到账，REFUND_QUERY-退款补查，COUPON_COMPENSATE-优惠券补偿，SMS_RETRY-短信重试',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `message_id` VARCHAR(64) DEFAULT NULL COMMENT '关联消息ID',
  `task_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待执行，PROCESSING-处理中，SUCCESS-成功，FAIL-失败，DEAD-已死信，CANCELLED-取消',
  `retry_count` INT NOT NULL DEFAULT '0' COMMENT '重试次数',
  `max_retry_count` INT NOT NULL DEFAULT '16' COMMENT '最大重试次数',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
  `last_error_message` VARCHAR(500) DEFAULT NULL COMMENT '最后一次错误信息',
  `extension_json` TEXT COMMENT '扩展信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_retry_task_no` (`task_no`),
  KEY `idx_retry_task_status` (`task_status`, `next_retry_time`),
  KEY `idx_retry_task_biz` (`biz_type`, `biz_no`),
  KEY `idx_retry_task_message` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重试任务表';
