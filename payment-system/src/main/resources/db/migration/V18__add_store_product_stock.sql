CREATE TABLE store_product_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 0 COMMENT '实物库存数量',
    locked_quantity INT NOT NULL DEFAULT 0 COMMENT '已锁定数量',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_product_stock (store_id, product_id),
    KEY idx_store_product_stock_tenant_store (tenant_id, store_id),
    KEY idx_store_product_stock_tenant_product (tenant_id, product_id)
) COMMENT='门店商品库存';

CREATE TABLE store_inventory_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    change_type VARCHAR(32) NOT NULL COMMENT 'ADJUST/LOCK/RELEASE/DEDUCT',
    change_quantity INT NOT NULL COMMENT '库存变化量，可为负数',
    quantity_before INT NOT NULL COMMENT '变化前实物库存',
    quantity_after INT NOT NULL COMMENT '变化后实物库存',
    locked_before INT NOT NULL COMMENT '变化前锁定库存',
    locked_after INT NOT NULL COMMENT '变化后锁定库存',
    biz_type VARCHAR(32) NULL COMMENT '业务类型',
    biz_no VARCHAR(64) NULL COMMENT '业务单号',
    operator_id BIGINT NULL COMMENT '操作人平台用户ID',
    remark VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_store_inventory_log_store_time (tenant_id, store_id, create_time),
    KEY idx_store_inventory_log_product_time (tenant_id, product_id, create_time),
    UNIQUE KEY uk_store_inventory_biz_change (store_id, product_id, change_type, biz_type, biz_no)
) COMMENT='门店库存变动流水';
