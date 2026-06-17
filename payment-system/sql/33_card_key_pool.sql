-- 卡密库存池：商户上传卡密，支付成功后按订单项锁定，退款撤销后作废为 RETURNED。

CREATE TABLE IF NOT EXISTS `card_key_pool` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `product_id` BIGINT(20) NOT NULL COMMENT '商品ID',
  `card_code` VARCHAR(255) NOT NULL COMMENT '卡密/兑换码',
  `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE-可用，USED-已售出，RETURNED-退款作废，DISABLED-停用',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '绑定订单号',
  `order_item_id` BIGINT(20) DEFAULT NULL COMMENT '绑定订单项ID',
  `used_time` DATETIME DEFAULT NULL COMMENT '售出时间',
  `returned_time` DATETIME DEFAULT NULL COMMENT '退回作废时间',
  `return_reason` VARCHAR(255) DEFAULT NULL COMMENT '退回原因',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_card_key_pool_code` (`tenant_id`, `product_id`, `card_code`),
  KEY `idx_card_key_pool_status` (`tenant_id`, `product_id`, `status`),
  KEY `idx_card_key_pool_order_item` (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡密库存池';
