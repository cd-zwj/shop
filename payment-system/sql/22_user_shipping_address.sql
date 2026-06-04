-- ========================================
-- 用户收货地址模块
-- ========================================

USE `payment_db`;

CREATE TABLE IF NOT EXISTS `user_shipping_address` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
  `city` VARCHAR(50) NOT NULL COMMENT '城市',
  `district` VARCHAR(50) DEFAULT NULL COMMENT '区县',
  `detail` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否默认地址：0-否，1-是',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_shipping_address_user` (`platform_user_id`, `deleted`, `is_default`),
  KEY `idx_user_shipping_address_update_time` (`platform_user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址表';
