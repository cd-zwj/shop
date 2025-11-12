-- ========================================
-- 商品模块
-- ========================================

USE `payment_db`;

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `product_code` VARCHAR(64) NOT NULL COMMENT '商品编码（条码/唯一标识）',
  `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '商品图片URL（OSS）',
  `description` TEXT COMMENT '商品描述',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：0-下架，1-上架',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_product_code` (`tenant_id`, `product_code`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_product_code` (`product_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 商品库存表
CREATE TABLE IF NOT EXISTS `product_stock` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `product_id` BIGINT(20) NOT NULL COMMENT '商品ID',
  `quantity` INT NOT NULL DEFAULT '0' COMMENT '库存数量',
  `version` INT NOT NULL DEFAULT '0' COMMENT '版本号（乐观锁）',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_id` (`product_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存表';
