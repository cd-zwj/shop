-- ========================================
-- 32. 到店自提凭证
-- 目的：支付成功后为实体商品生成到店自提凭证。
-- 不支持虚拟商品、卡密、订阅、服务核销或物流交付。
-- ========================================

USE payment_db;

ALTER TABLE sales_order_item
    ADD COLUMN delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
        COMMENT '履约状态：PENDING/DELIVERING/DELIVERED/CONFIRMED/FAILED/REVOKED',
    ADD COLUMN delivered_time DATETIME NULL COMMENT '取货凭证生成时间',
    ADD INDEX idx_delivery_status (delivery_status);

CREATE TABLE IF NOT EXISTS order_delivery_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    order_item_id BIGINT NOT NULL COMMENT '订单项ID',
    platform_user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NULL COMMENT '商品名称快照',
    status VARCHAR(32) NOT NULL COMMENT '自提凭证状态：DELIVERED/CONFIRMED/REVOKED/FAILED',
    payload TEXT NOT NULL COMMENT 'JSON 自提凭证：pickupCode、storeId',
    pickup_code_hash VARCHAR(64) NULL COMMENT '取货码 SHA-256 哈希（hex），核销校验与唯一性依据',
    store_id BIGINT NULL COMMENT '自提门店 ID',
    verified_by BIGINT NULL COMMENT '核销人（平台用户 ID）',
    fail_reason VARCHAR(500) NULL COMMENT '失败原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    delivered_time DATETIME NULL COMMENT '取货凭证生成时间',
    confirmed_time DATETIME NULL COMMENT '店员核销时间',
    revoked_time DATETIME NULL COMMENT '退款撤销时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_user (tenant_id, platform_user_id),
    INDEX idx_order (order_id),
    UNIQUE KEY uk_tenant_item (tenant_id, order_item_id),
    UNIQUE KEY uk_tenant_pickup_hash (tenant_id, pickup_code_hash),
    INDEX idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='到店自提凭证记录';
