-- ========================================
-- 用户通知模块
-- 当前阶段仅提供只读列表与已读状态，不引入推送链路
-- ========================================

USE `payment_db`;

CREATE TABLE IF NOT EXISTS `user_notification` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(500) NOT NULL COMMENT '通知内容',
  `category` VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '通知分类：SYSTEM-系统，ORDER-订单，WALLET-钱包，BENEFIT-权益',
  `read_status` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '已读状态：0-未读，1-已读',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `read_time` DATETIME DEFAULT NULL COMMENT '已读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_notification_user_read` (`platform_user_id`, `read_status`, `create_time`),
  KEY `idx_user_notification_category` (`category`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';
