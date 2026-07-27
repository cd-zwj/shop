-- ========================================
-- v1 平台用户 + 双钱包 + 支付单模型
-- ========================================

USE `payment_db`;

CREATE TABLE IF NOT EXISTS `platform_user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_no` VARCHAR(32) NOT NULL COMMENT '平台用户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-否, 1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户主表';

CREATE TABLE IF NOT EXISTS `platform_user_auth` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `auth_type` VARCHAR(32) NOT NULL COMMENT '认证类型',
  `auth_key` VARCHAR(128) NOT NULL COMMENT '认证唯一键',
  `auth_union_key` VARCHAR(128) DEFAULT NULL COMMENT '联合认证键',
  `extra_json` TEXT COMMENT '扩展信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_type_key` (`auth_type`, `auth_key`),
  KEY `idx_platform_user_id` (`platform_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户认证绑定表';

CREATE TABLE IF NOT EXISTS `tenant_employee` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `employee_no` VARCHAR(32) NOT NULL COMMENT '员工编号',
  `employee_role` VARCHAR(32) NOT NULL COMMENT '员工角色',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_platform_user` (`tenant_id`, `platform_user_id`),
  UNIQUE KEY `uk_employee_no` (`employee_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户员工关系表';

CREATE TABLE IF NOT EXISTS `tenant_member` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `member_no` VARCHAR(32) NOT NULL COMMENT '会员编号',
  `member_status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '会员状态: 0-禁用, 1-启用',
  `member_level` INT DEFAULT 1 COMMENT '会员等级（关联 member_level.id）',
  `register_source` VARCHAR(32) DEFAULT NULL COMMENT '注册来源',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_member` (`tenant_id`, `platform_user_id`),
  UNIQUE KEY `uk_member_no` (`member_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户会员关系表';

CREATE TABLE IF NOT EXISTS `unified_wallet_account` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `available_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额',
  `frozen_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '冻结余额',
  `total_recharge` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计充值',
  `total_consume` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计消费',
  `version` INT NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_user` (`platform_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一钱包账户表';

CREATE TABLE IF NOT EXISTS `unified_wallet_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `change_amount` DECIMAL(18,2) NOT NULL COMMENT '变动金额',
  `balance_before` DECIMAL(18,2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(18,2) NOT NULL COMMENT '变动后余额',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`platform_user_id`, `create_time`),
  KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一钱包流水表';

CREATE TABLE IF NOT EXISTS `merchant_wallet_account` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `available_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额',
  `frozen_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '冻结余额',
  `total_recharge` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计充值',
  `total_consume` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计消费',
  `version` INT NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_platform_user_wallet` (`tenant_id`, `platform_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户钱包账户表';

CREATE TABLE IF NOT EXISTS `merchant_wallet_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `change_amount` DECIMAL(18,2) NOT NULL COMMENT '变动金额',
  `balance_before` DECIMAL(18,2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(18,2) NOT NULL COMMENT '变动后余额',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user_time` (`tenant_id`, `platform_user_id`, `create_time`),
  KEY `idx_wallet_biz_no` (`biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户钱包流水表';

CREATE TABLE IF NOT EXISTS `sales_order` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `order_status` VARCHAR(32) NOT NULL COMMENT '订单状态',
  `pay_status` VARCHAR(32) NOT NULL COMMENT '支付状态',
  `total_amount` DECIMAL(18,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '优惠金额',
  `wallet_deduct_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '钱包合计抵扣',
  `unified_wallet_deduct_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '统一钱包抵扣',
  `merchant_wallet_deduct_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '商户钱包抵扣',
  `external_pay_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '外部支付金额',
  `payable_amount` DECIMAL(18,2) NOT NULL COMMENT '应付金额',
  `subject` VARCHAR(200) NOT NULL COMMENT '订单标题',
  `source` VARCHAR(32) DEFAULT NULL COMMENT '订单来源',
  `wallet_strategy` VARCHAR(32) NOT NULL COMMENT '钱包扣款策略',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no_v1` (`order_no`),
  KEY `idx_sales_tenant_user` (`tenant_id`, `platform_user_id`),
  KEY `idx_sales_order_expire_scan` (`order_status`, `pay_status`, `expire_time`, `id`),
  KEY `idx_sales_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费订单表';

CREATE TABLE IF NOT EXISTS `sales_order_item` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT(20) NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `product_id` BIGINT(20) NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `price` DECIMAL(18,2) NOT NULL COMMENT '单价',
  `quantity` INT NOT NULL COMMENT '数量',
  `subtotal` DECIMAL(18,2) NOT NULL COMMENT '小计',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sales_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费订单明细表';

CREATE TABLE IF NOT EXISTS `merchant_recharge_rule` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `recharge_amount` DECIMAL(18,2) NOT NULL COMMENT '充值金额',
  `gift_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '赠送金额',
  `gift_points` INT NOT NULL DEFAULT '0' COMMENT '赠送积分',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `sort_order` INT NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_recharge_rule_tenant` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户钱包充值规则表';

CREATE TABLE IF NOT EXISTS `recharge_order_v1` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recharge_no` VARCHAR(64) NOT NULL COMMENT '充值单号',
  `wallet_type` VARCHAR(32) NOT NULL COMMENT '钱包类型',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `rule_id` BIGINT(20) DEFAULT NULL COMMENT '规则ID',
  `recharge_amount` DECIMAL(18,2) NOT NULL COMMENT '充值金额',
  `gift_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '赠送金额',
  `gift_points` INT NOT NULL DEFAULT '0' COMMENT '赠送积分',
  `actual_credit_amount` DECIMAL(18,2) NOT NULL COMMENT '实际入账金额',
  `biz_status` VARCHAR(32) NOT NULL COMMENT '业务状态',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recharge_no_v1` (`recharge_no`),
  KEY `idx_recharge_v1_user` (`platform_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='v1充值业务单表';

CREATE TABLE IF NOT EXISTS `payment_bill` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `channel_code` VARCHAR(32) NOT NULL COMMENT '渠道编码',
  `channel_mode` VARCHAR(32) DEFAULT NULL COMMENT '渠道模式',
  `pay_amount` DECIMAL(18,2) NOT NULL COMMENT '支付金额',
  `pay_status` VARCHAR(32) NOT NULL COMMENT '支付状态',
  `third_party_bill_no` VARCHAR(128) DEFAULT NULL COMMENT '第三方支付单号',
  `callback_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_CALLBACK' COMMENT '回调状态',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `extension_json` TEXT COMMENT '扩展信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  UNIQUE KEY `uk_biz_type_biz_no` (`biz_type`, `biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一支付单表';

CREATE TABLE IF NOT EXISTS `payment_callback_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
  `channel_code` VARCHAR(32) NOT NULL COMMENT '渠道编码',
  `callback_request_id` VARCHAR(128) NOT NULL COMMENT '回调请求ID',
  `callback_body` TEXT COMMENT '已验签回调报文摘要与字节数',
  `verify_status` VARCHAR(32) NOT NULL COMMENT '验签状态',
  `process_status` VARCHAR(32) NOT NULL COMMENT '处理状态',
  `callback_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回调时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_request` (`channel_code`, `callback_request_id`),
  KEY `idx_callback_bill_no` (`bill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调记录表';

CREATE TABLE IF NOT EXISTS `payment_callback_failure_audit` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` VARCHAR(64) NOT NULL COMMENT '服务端生成的审计事件ID',
  `channel_code` VARCHAR(32) NOT NULL COMMENT '归一化支付渠道',
  `failure_reason` VARCHAR(64) NOT NULL COMMENT '固定枚举拒绝原因',
  `verify_status` VARCHAR(32) NOT NULL COMMENT '签名验证状态',
  `candidate_bill_no` VARCHAR(64) DEFAULT NULL COMMENT '未信任的候选支付单号',
  `provider_request_id` VARCHAR(128) DEFAULT NULL COMMENT '未信任的渠道请求ID',
  `payload_sha256` CHAR(64) NOT NULL COMMENT '原始报文SHA-256，不保存报文原文',
  `payload_size` INT NOT NULL COMMENT '原始报文字节数',
  `occurrence_count` BIGINT NOT NULL DEFAULT 1 COMMENT '窗口内同类拒绝次数',
  `window_start` DATETIME NOT NULL COMMENT '分钟聚合窗口',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
  `last_time` DATETIME NOT NULL COMMENT '最近一次接收时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期清理时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_callback_failure_event` (`event_id`),
  UNIQUE KEY `uk_callback_failure_window` (`channel_code`, `failure_reason`, `window_start`),
  KEY `idx_callback_failure_expire` (`expire_time`),
  KEY `idx_callback_failure_digest` (`channel_code`, `payload_sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调拒绝安全审计表';

CREATE TABLE IF NOT EXISTS `member_points_account` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `points` INT NOT NULL DEFAULT '0' COMMENT '当前积分',
  `total_earned` INT NOT NULL DEFAULT '0' COMMENT '累计获得',
  `total_used` INT NOT NULL DEFAULT '0' COMMENT '累计使用',
  `version` INT NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_points` (`tenant_id`, `platform_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户会员积分账户表';

CREATE TABLE IF NOT EXISTS `member_points_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `change_points` INT NOT NULL COMMENT '积分变动',
  `points_before` INT NOT NULL COMMENT '变动前积分',
  `points_after` INT NOT NULL COMMENT '变动后积分',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `status` VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED' COMMENT '状态：PRE_HOLD-预占，CONFIRMED-确认，RELEASED-释放，EXPIRED-已过期',
  `expire_time` DATETIME DEFAULT NULL COMMENT '积分过期时间',
  `confirm_time` DATETIME DEFAULT NULL COMMENT '确认时间',
  `release_time` DATETIME DEFAULT NULL COMMENT '释放时间',
  `release_reason` VARCHAR(255) DEFAULT NULL COMMENT '释放原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_points_tenant_user_time` (`tenant_id`, `platform_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户会员积分流水表';

CREATE TABLE IF NOT EXISTS `message_outbox` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `exchange_name` VARCHAR(64) NOT NULL COMMENT '交换机',
  `routing_key` VARCHAR(64) NOT NULL COMMENT '路由键',
  `message_body` TEXT NOT NULL COMMENT '消息体',
  `send_status` VARCHAR(32) NOT NULL COMMENT '发送状态',
  `retry_count` INT NOT NULL DEFAULT '0' COMMENT '重试次数',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbox_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表';

CREATE TABLE IF NOT EXISTS `compensation_task` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_no` VARCHAR(64) NOT NULL COMMENT '任务编号',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `task_status` VARCHAR(32) NOT NULL COMMENT '任务状态',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `retry_count` INT NOT NULL DEFAULT '0' COMMENT '重试次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_compensation_task_no` (`task_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补偿任务表';

CREATE TABLE IF NOT EXISTS `dead_letter_task` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID',
  `queue_name` VARCHAR(64) NOT NULL COMMENT '队列名',
  `message_body` TEXT COMMENT '消息体',
  `fail_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dead_letter_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信任务表';

-- ========================================
-- 兼容已有数据库：给 tenant_member 补 member_level 列
-- ========================================
ALTER TABLE `tenant_member`
  ADD COLUMN IF NOT EXISTS `member_level` INT DEFAULT 1 COMMENT '会员等级（关联 member_level.id）';

-- ========================================
-- 兼容已有数据库：给 member_points_log 补积分过期时间
-- ========================================
ALTER TABLE `member_points_log`
  ADD COLUMN IF NOT EXISTS `expire_time` DATETIME DEFAULT NULL COMMENT '积分过期时间';
