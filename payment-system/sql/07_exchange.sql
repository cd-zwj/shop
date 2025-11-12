-- ========================================
-- 积分兑换模块
-- ========================================

USE `payment_db`;

-- 积分兑换商品表
CREATE TABLE IF NOT EXISTS `exchange_product` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `product_image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
  `points_required` INT NOT NULL COMMENT '所需积分',
  `stock` INT NOT NULL DEFAULT '0' COMMENT '库存数量',
  `exchange_limit` INT DEFAULT NULL COMMENT '兑换限制（每人）',
  `description` TEXT COMMENT '商品描述',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：0-下架，1-上架',
  `sort_order` INT NOT NULL DEFAULT '0' COMMENT '排序',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分兑换商品表';
