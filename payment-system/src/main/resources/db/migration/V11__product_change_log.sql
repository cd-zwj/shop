CREATE TABLE IF NOT EXISTS product_change_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  change_type VARCHAR(32) NOT NULL COMMENT '变更类型：PRICE/STOCK',
  field_name VARCHAR(64) NOT NULL COMMENT '变更字段：price/stock',
  old_value VARCHAR(128) DEFAULT NULL COMMENT '变更前值',
  new_value VARCHAR(128) DEFAULT NULL COMMENT '变更后值',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人平台用户ID',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_tenant_product_time (tenant_id, product_id, create_time),
  KEY idx_tenant_operator_time (tenant_id, operator_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品价格库存变更流水';
