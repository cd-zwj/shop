-- ========================================
-- 26. 优惠券记录 & 活动规则 & 订单优惠快照
-- 补充 Entity 对应的建表 DDL + merchant_balance 乐观锁字段
-- ========================================

USE `payment_db`;

-- ========================================
-- A. merchant_balance 补充乐观锁 version 字段
-- ========================================

SET @merchant_balance_version_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'merchant_balance'
      AND column_name = 'version'
);
SET @merchant_balance_version_sql = IF(
    @merchant_balance_version_exists = 0,
    'ALTER TABLE `merchant_balance` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `deleted`',
    'SELECT 1'
);
PREPARE merchant_balance_version_stmt FROM @merchant_balance_version_sql;
EXECUTE merchant_balance_version_stmt;
DEALLOCATE PREPARE merchant_balance_version_stmt;

-- ========================================
-- B. 优惠券生命周期记录表（8 张）
-- ========================================

-- 1. 优惠券适用范围表
CREATE TABLE IF NOT EXISTS `coupon_scope` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coupon_template_id` BIGINT(20) NOT NULL COMMENT '优惠券模板ID',
  `scope_type` VARCHAR(20) NOT NULL COMMENT '范围类型：PRODUCT-指定商品，CATEGORY-指定分类',
  `scope_id` BIGINT(20) DEFAULT NULL COMMENT '范围对象ID（商品ID或分类ID）',
  `scope_code` VARCHAR(64) DEFAULT NULL COMMENT '范围编码（商品编码或分类编码）',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_scope_template` (`coupon_template_id`),
  KEY `idx_coupon_scope_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券适用范围表';

-- 2. 优惠券过期记录表
CREATE TABLE IF NOT EXISTS `coupon_expire_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_coupon_id` BIGINT(20) NOT NULL COMMENT '用户优惠券ID',
  `coupon_template_id` BIGINT(20) NOT NULL COMMENT '优惠券模板ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
  `expire_reason` VARCHAR(64) DEFAULT NULL COMMENT '过期原因：AUTO-自动过期，MANUAL-手动作废',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_expire_user_coupon` (`user_coupon_id`),
  KEY `idx_coupon_expire_tenant` (`tenant_id`),
  KEY `idx_coupon_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券过期记录表';

-- 3. 优惠券锁定记录表
CREATE TABLE IF NOT EXISTS `coupon_lock_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_coupon_id` BIGINT(20) NOT NULL COMMENT '用户优惠券ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) DEFAULT NULL COMMENT '平台用户ID',
  `order_id` BIGINT(20) DEFAULT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
  `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
  `lock_time` DATETIME NOT NULL COMMENT '锁定时间',
  `lock_status` VARCHAR(20) NOT NULL DEFAULT 'LOCKED' COMMENT '锁定状态：LOCKED-已锁定，RELEASED-已释放',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_lock_user_coupon` (`user_coupon_id`),
  KEY `idx_coupon_lock_tenant` (`tenant_id`),
  KEY `idx_coupon_lock_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券锁定记录表';

-- 4. 优惠券领取记录表
CREATE TABLE IF NOT EXISTS `coupon_receive_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_coupon_id` BIGINT(20) NOT NULL COMMENT '用户优惠券ID',
  `coupon_template_id` BIGINT(20) NOT NULL COMMENT '优惠券模板ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
  `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_receive_user_coupon` (`user_coupon_id`),
  KEY `idx_coupon_receive_tenant` (`tenant_id`),
  KEY `idx_coupon_receive_user` (`platform_user_id`, `receive_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券领取记录表';

-- 5. 优惠券释放记录表
CREATE TABLE IF NOT EXISTS `coupon_release_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_coupon_id` BIGINT(20) NOT NULL COMMENT '用户优惠券ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) DEFAULT NULL COMMENT '平台用户ID',
  `order_id` BIGINT(20) DEFAULT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
  `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
  `release_reason` VARCHAR(64) DEFAULT NULL COMMENT '释放原因：ORDER_CANCEL-订单取消，ORDER_REFUND-订单退款，TIMEOUT-超时释放',
  `release_time` DATETIME NOT NULL COMMENT '释放时间',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_release_user_coupon` (`user_coupon_id`),
  KEY `idx_coupon_release_tenant` (`tenant_id`),
  KEY `idx_coupon_release_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券释放记录表';

-- 6. 优惠券核销记录表
CREATE TABLE IF NOT EXISTS `coupon_write_off_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_coupon_id` BIGINT(20) NOT NULL COMMENT '用户优惠券ID',
  `coupon_template_id` BIGINT(20) NOT NULL COMMENT '优惠券模板ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) DEFAULT NULL COMMENT '平台用户ID',
  `order_id` BIGINT(20) DEFAULT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
  `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
  `discount_amount` DECIMAL(18,2) NOT NULL COMMENT '核销抵扣金额',
  `write_off_time` DATETIME NOT NULL COMMENT '核销时间',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_write_off_user_coupon` (`user_coupon_id`),
  KEY `idx_coupon_write_off_tenant` (`tenant_id`),
  KEY `idx_coupon_write_off_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券核销记录表';

-- 7. 营销活动规则表
CREATE TABLE IF NOT EXISTS `activity_rule` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT(20) NOT NULL COMMENT '营销活动ID',
  `rule_type` VARCHAR(20) NOT NULL COMMENT '规则类型：FULL_REDUCTION-满减，DISCOUNT-折扣',
  `threshold_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '满减门槛金额',
  `discount_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '固定优惠金额',
  `discount_rate` DECIMAL(8,4) DEFAULT NULL COMMENT '折扣比例，例如0.8500表示85折',
  `product_id` BIGINT(20) DEFAULT NULL COMMENT '指定商品ID',
  `category_code` VARCHAR(64) DEFAULT NULL COMMENT '指定分类编码',
  `rule_config_json` TEXT COMMENT '扩展规则配置JSON',
  `priority` INT NOT NULL DEFAULT '0' COMMENT '优先级，数值越大优先级越高',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_activity_rule_activity` (`activity_id`, `deleted`),
  KEY `idx_activity_rule_priority` (`activity_id`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动规则表';

-- 8. 订单优惠快照表
CREATE TABLE IF NOT EXISTS `order_discount_snapshot` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT(20) NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `activity_id` BIGINT(20) DEFAULT NULL COMMENT '营销活动ID',
  `activity_rule_id` BIGINT(20) DEFAULT NULL COMMENT '活动规则ID',
  `user_coupon_id` BIGINT(20) DEFAULT NULL COMMENT '用户优惠券ID',
  `coupon_template_id` BIGINT(20) DEFAULT NULL COMMENT '优惠券模板ID',
  `discount_source` VARCHAR(20) NOT NULL COMMENT '优惠来源：ACTIVITY-活动，COUPON-优惠券',
  `discount_type` VARCHAR(20) NOT NULL COMMENT '优惠类型：FULL_REDUCTION-满减，DISCOUNT-折扣，CASH-现金券',
  `discount_amount` DECIMAL(18,2) NOT NULL COMMENT '优惠金额',
  `rule_snapshot_json` TEXT COMMENT '规则快照JSON，冻结下单时刻的规则',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_discount_snapshot_order` (`order_id`),
  KEY `idx_discount_snapshot_order_no` (`order_no`),
  KEY `idx_discount_snapshot_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单优惠快照表';
