-- ========================================
-- 数据分析模块
-- ========================================

USE `payment_db`;

-- 用户行为日志表
CREATE TABLE IF NOT EXISTS `user_behavior_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `behavior_type` VARCHAR(50) NOT NULL COMMENT '行为类型：LOGIN-登录，PAY-支付，VIEW-浏览，SEARCH-搜索，SCAN-扫码',
  `behavior_data` TEXT COMMENT '行为数据JSON',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_behavior_type` (`behavior_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为日志表';

-- 数据分析结果表
CREATE TABLE IF NOT EXISTS `data_analysis_result` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `analysis_type` VARCHAR(50) NOT NULL COMMENT '分析类型：USER_BEHAVIOR-用户行为，PAYMENT_TREND-支付趋势，USER_SEGMENT-用户分群',
  `analysis_data` TEXT COMMENT '分析数据JSON',
  `chart_url` VARCHAR(500) DEFAULT NULL COMMENT '图表URL',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING-处理中，SUCCESS-成功，FAIL-失败',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_analysis_type` (`analysis_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据分析结果表';
