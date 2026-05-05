-- ========================================
-- 优惠券与营销活动模块
-- 基于 v1 平台用户 / 统一支付模型扩展
-- ========================================

USE `payment_db`;

-- 优惠券模板表
CREATE TABLE IF NOT EXISTS `coupon_template` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_no` VARCHAR(64) NOT NULL COMMENT '模板编号',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID，平台券可为空',
  `template_scope` VARCHAR(20) NOT NULL DEFAULT 'TENANT' COMMENT '模板范围：PLATFORM-平台券，TENANT-商户券',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `coupon_type` VARCHAR(20) NOT NULL COMMENT '优惠券类型：FULL_REDUCTION-满减券，DISCOUNT-折扣券，CASH-无门槛券，NEW_USER-新用户券，RECHARGE_GIFT-充值赠券，ACTIVITY-活动券',
  `threshold_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '使用门槛金额',
  `discount_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '固定优惠金额',
  `discount_rate` DECIMAL(8,4) DEFAULT NULL COMMENT '折扣比例，例如0.8500表示85折',
  `max_discount_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '折扣券最高优惠金额',
  `total_quantity` INT NOT NULL DEFAULT '0' COMMENT '发行总量，0表示不限制',
  `received_quantity` INT NOT NULL DEFAULT '0' COMMENT '已领取数量',
  `used_quantity` INT NOT NULL DEFAULT '0' COMMENT '已使用数量',
  `per_user_limit` INT NOT NULL DEFAULT '1' COMMENT '每人限领张数',
  `receive_start_time` DATETIME DEFAULT NULL COMMENT '领取开始时间',
  `receive_end_time` DATETIME DEFAULT NULL COMMENT '领取结束时间',
  `valid_type` VARCHAR(20) NOT NULL DEFAULT 'FIXED_DAYS' COMMENT '有效期类型：FIXED_DAYS-领券后固定天数，FIXED_RANGE-固定时间范围',
  `valid_days` INT DEFAULT NULL COMMENT '领券后有效天数',
  `valid_start_time` DATETIME DEFAULT NULL COMMENT '固定有效期开始时间',
  `valid_end_time` DATETIME DEFAULT NULL COMMENT '固定有效期结束时间',
  `can_stack_balance` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '是否允许与余额叠加',
  `can_stack_points` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '是否允许与积分叠加',
  `can_stack_other_coupon` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否允许与其他优惠券叠加',
  `applicable_product_scope` VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '适用商品范围：ALL-全部，INCLUDE-指定可用，EXCLUDE-指定不可用',
  `applicable_product_json` TEXT COMMENT '适用商品范围JSON',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿，ACTIVE-生效，DISABLED-停用，EXPIRED-已过期',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_template_no` (`template_no`),
  KEY `idx_coupon_template_tenant` (`tenant_id`, `status`),
  KEY `idx_coupon_template_scope` (`template_scope`, `coupon_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- 用户领券表
CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coupon_no` VARCHAR(64) NOT NULL COMMENT '用户券编号',
  `template_id` BIGINT(20) NOT NULL COMMENT '模板ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `source_type` VARCHAR(20) NOT NULL COMMENT '来源：RECEIVE-主动领取，GRANT-后台发放，RECHARGE-充值赠送，ACTIVITY-活动发放',
  `source_biz_no` VARCHAR(64) DEFAULT NULL COMMENT '来源业务单号',
  `coupon_status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE-可用，LOCKED-已锁定，USED-已使用，EXPIRED-已过期，VOID-已作废',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `lock_time` DATETIME DEFAULT NULL COMMENT '锁定时间',
  `use_time` DATETIME DEFAULT NULL COMMENT '使用时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `version` INT NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon_no` (`coupon_no`),
  KEY `idx_user_coupon_user_status` (`platform_user_id`, `coupon_status`, `expire_time`),
  KEY `idx_user_coupon_tenant_template` (`tenant_id`, `template_id`),
  KEY `idx_user_coupon_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- 优惠券操作日志表
CREATE TABLE IF NOT EXISTS `coupon_operation_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coupon_no` VARCHAR(64) NOT NULL COMMENT '用户券编号',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型：RECEIVE-领取，LOCK-锁定，UNLOCK-解锁，USE-核销，EXPIRE-过期，VOID-作废',
  `before_status` VARCHAR(20) DEFAULT NULL COMMENT '变更前状态',
  `after_status` VARCHAR(20) DEFAULT NULL COMMENT '变更后状态',
  `biz_type` VARCHAR(20) DEFAULT NULL COMMENT '业务类型：SALES_ORDER-消费订单，RECHARGE_ORDER-充值订单，ACTIVITY-活动',
  `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_operation_coupon` (`coupon_no`, `create_time`),
  KEY `idx_coupon_operation_user` (`platform_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券操作日志表';

-- 订单优惠明细表
CREATE TABLE IF NOT EXISTS `order_coupon_detail` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `coupon_no` VARCHAR(64) NOT NULL COMMENT '用户券编号',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `template_id` BIGINT(20) NOT NULL COMMENT '模板ID',
  `discount_amount` DECIMAL(18,2) NOT NULL COMMENT '抵扣金额',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_coupon_no` (`order_no`, `coupon_no`),
  KEY `idx_order_coupon_user` (`platform_user_id`, `create_time`),
  KEY `idx_order_coupon_tenant` (`tenant_id`, `order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单优惠券明细表';

-- 营销活动表
CREATE TABLE IF NOT EXISTS `marketing_activity` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_no` VARCHAR(64) NOT NULL COMMENT '活动编号',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID，平台活动可为空',
  `activity_scope` VARCHAR(20) NOT NULL DEFAULT 'TENANT' COMMENT '活动范围：PLATFORM-平台活动，TENANT-商户活动',
  `activity_name` VARCHAR(100) NOT NULL COMMENT '活动名称',
  `activity_type` VARCHAR(20) NOT NULL COMMENT '活动类型：COUPON-发券，FULL_REDUCTION-满减，DISCOUNT-折扣，RECHARGE_GIFT-充值赠送',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿，PENDING-待生效，ACTIVE-进行中，FINISHED-已结束，DISABLED-停用',
  `rule_json` TEXT COMMENT '活动规则JSON',
  `grant_coupon_template_id` BIGINT(20) DEFAULT NULL COMMENT '发放的优惠券模板ID',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '活动说明',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_marketing_activity_no` (`activity_no`),
  KEY `idx_marketing_activity_tenant` (`tenant_id`, `status`),
  KEY `idx_marketing_activity_type` (`activity_type`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动表';

-- 活动发放记录表
CREATE TABLE IF NOT EXISTS `activity_grant_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_no` VARCHAR(64) NOT NULL COMMENT '活动编号',
  `coupon_no` VARCHAR(64) DEFAULT NULL COMMENT '发放的用户券编号',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `grant_status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '发放状态：SUCCESS-成功，FAIL-失败，PENDING-处理中',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `grant_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发放时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user` (`activity_no`, `platform_user_id`),
  KEY `idx_activity_grant_user` (`platform_user_id`, `grant_time`),
  KEY `idx_activity_grant_tenant` (`tenant_id`, `grant_status`),
  KEY `idx_activity_grant_coupon` (`coupon_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动发放记录表';
