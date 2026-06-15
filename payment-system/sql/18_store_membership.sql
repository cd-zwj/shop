-- ========================================
-- 门店与会员扩展模块
-- 基于多租户经营与 v1 平台用户模型扩展
-- 当前阶段仅落地主数据与会员能力，不引入新的商品分类/库存真相源
-- ========================================

USE `payment_db`;

-- 门店表
CREATE TABLE IF NOT EXISTS `store` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_no` VARCHAR(64) NOT NULL COMMENT '门店编号',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `store_name` VARCHAR(100) NOT NULL COMMENT '门店名称',
  `store_type` VARCHAR(20) NOT NULL DEFAULT 'DIRECT' COMMENT '门店类型：DIRECT-直营，FRANCHISE-加盟',
  `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `district` VARCHAR(50) DEFAULT NULL COMMENT '区县',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `business_hours` VARCHAR(255) DEFAULT NULL COMMENT '营业时间描述',
  `service_tags` VARCHAR(255) DEFAULT NULL COMMENT '服务标签，逗号分隔',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_no` (`store_no`),
  KEY `idx_store_tenant_status` (`tenant_id`, `status`),
  KEY `idx_store_city` (`city`, `district`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店表';

-- 会员等级表
CREATE TABLE IF NOT EXISTS `member_level` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `level_no` VARCHAR(64) NOT NULL COMMENT '等级编号',
  `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
  `level_rank` INT NOT NULL COMMENT '等级排序值，越大等级越高',
  `upgrade_growth` INT NOT NULL DEFAULT '0' COMMENT '升级所需成长值',
  `downgrade_growth` INT NOT NULL DEFAULT '0' COMMENT '降级阈值，成长值低于该值时降级',
  `level_validity_days` INT DEFAULT NULL COMMENT '等级有效期天数，NULL表示长期有效',
  `discount_rate` DECIMAL(8,4) DEFAULT NULL COMMENT '等级折扣率，例如0.9500表示95折',
  `benefit_json` TEXT COMMENT '权益说明JSON',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_level_no` (`level_no`),
  UNIQUE KEY `uk_member_level_rank` (`tenant_id`, `level_rank`),
  KEY `idx_member_level_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级表';

-- 会员标签表
CREATE TABLE IF NOT EXISTS `member_tag` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `tag_code` VARCHAR(64) NOT NULL COMMENT '标签编码',
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `tag_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '标签类型：MANUAL-手动，RULE-规则，SYSTEM-系统',
  `tag_color` VARCHAR(20) DEFAULT NULL COMMENT '标签颜色',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '标签描述',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_tag_code` (`tag_code`),
  UNIQUE KEY `uk_member_tag_name` (`tenant_id`, `tag_name`),
  KEY `idx_member_tag_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员标签表';

-- 会员标签关联表
CREATE TABLE IF NOT EXISTS `member_tag_relation` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `tag_id` BIGINT(20) NOT NULL COMMENT '标签ID',
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL-手动，RULE-规则，SYSTEM-系统',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_tag_relation` (`tenant_id`, `platform_user_id`, `tag_id`),
  KEY `idx_member_tag_relation_tag` (`tag_id`, `create_time`),
  KEY `idx_member_tag_relation_user` (`platform_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员标签关联表';

-- 会员成长值日志表
CREATE TABLE IF NOT EXISTS `member_growth_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '商户ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `change_type` VARCHAR(20) NOT NULL COMMENT '变动类型：EARN-增加，DEDUCT-扣减，ADJUST-调整',
  `change_growth` INT NOT NULL COMMENT '变动成长值',
  `growth_before` INT NOT NULL COMMENT '变动前成长值',
  `growth_after` INT NOT NULL COMMENT '变动后成长值',
  `level_id` BIGINT(20) DEFAULT NULL COMMENT '关联等级ID',
  `biz_type` VARCHAR(20) DEFAULT NULL COMMENT '业务类型：ORDER-订单，RECHARGE-充值，MANUAL-人工',
  `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_growth_user` (`tenant_id`, `platform_user_id`, `create_time`),
  KEY `idx_member_growth_biz` (`biz_type`, `biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员成长值日志表';

-- 兼容已有数据库：给 member_level 补降级与有效期字段
ALTER TABLE `member_level`
  ADD COLUMN IF NOT EXISTS `downgrade_growth` INT NOT NULL DEFAULT '0' COMMENT '降级阈值，成长值低于该值时降级',
  ADD COLUMN IF NOT EXISTS `level_validity_days` INT DEFAULT NULL COMMENT '等级有效期天数，NULL表示长期有效';
