CREATE TABLE store_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    product_id BIGINT NOT NULL COMMENT '租户级商品ID',
    price DECIMAL(10,2) NULL COMMENT '门店售价，空值使用商品基础售价',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '门店上架状态：0下架，1上架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_product (store_id, product_id),
    KEY idx_store_product_tenant_store_status (tenant_id, store_id, status),
    KEY idx_store_product_tenant_product (tenant_id, product_id)
) COMMENT='门店商品关联';
