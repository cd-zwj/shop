-- ========================================
-- 32. 商品类型化与统一交付框架
-- 目的：让"下单→支付→交付"这条主干在多种商品类型（实物/虚拟/卡密/服务/订阅）下都跑通
-- 设计：商品加 product_type 字段；订单项加 delivery_status 按 item 跟踪；新增 order_delivery_record 统一记录表
-- 兼容：存量商品/订单项默认按实物处理，SQL 末尾回填
-- ========================================

USE payment_db;

-- ----------------------------------------
-- 1. 商品表加类型与交付配置
-- ----------------------------------------
ALTER TABLE product
    ADD COLUMN product_type VARCHAR(32) NOT NULL DEFAULT 'PHYSICAL'
        COMMENT '商品类型: PHYSICAL/VIRTUAL/CARD_KEY/SERVICE/SUBSCRIPTION',
    ADD COLUMN delivery_config TEXT NULL
        COMMENT '交付配置(JSON,按类型解读): VIRTUAL={contentUrl}, SUBSCRIPTION={validityDays}, 等等',
    ADD INDEX idx_product_type (tenant_id, product_type);

-- ----------------------------------------
-- 2. 订单项加交付状态字段（按 item 跟踪，同一订单可混合多种商品）
-- ----------------------------------------
ALTER TABLE sales_order_item
    ADD COLUMN product_type VARCHAR(32) NULL
        COMMENT '冗余商品类型，避免商品后续改类型时影响历史订单',
    ADD COLUMN delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
        COMMENT '交付状态: PENDING/DELIVERING/DELIVERED/CONFIRMED/FAILED/REVOKED/REVOKE_FAILED',
    ADD COLUMN delivered_time DATETIME NULL COMMENT '交付完成时间',
    ADD INDEX idx_delivery_status (delivery_status);

-- ----------------------------------------
-- 3. 统一交付记录表（所有商品类型共用）
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS order_delivery_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    order_item_id BIGINT NOT NULL COMMENT '订单项ID',
    platform_user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NULL COMMENT '商品名称快照',
    product_type VARCHAR(32) NOT NULL COMMENT '商品类型',
    status VARCHAR(32) NOT NULL COMMENT '状态: PENDING/DELIVERED/CONFIRMED/REVOKED/REVOKE_FAILED/FAILED',
    payload TEXT NULL COMMENT 'JSON 交付内容: 卡密=code, 虚拟=url, 服务=核销码, 实物=物流单号',
    fail_reason VARCHAR(500) NULL COMMENT '失败原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    delivered_time DATETIME NULL COMMENT '交付时间',
    confirmed_time DATETIME NULL COMMENT '用户确认收货时间',
    expire_time DATETIME NULL COMMENT '权益到期时间(订阅类用)',
    revoked_time DATETIME NULL COMMENT '退款回收时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_user (tenant_id, platform_user_id),
    INDEX idx_order (order_id),
    UNIQUE KEY uk_tenant_item (tenant_id, order_item_id),
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_user_status (platform_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='订单交付记录(所有商品类型共用,payload 按 product_type 解读)';

-- ----------------------------------------
-- 4. 存量数据回填
-- ----------------------------------------
-- 商品默认实物
UPDATE product SET product_type = 'PHYSICAL'
    WHERE product_type IS NULL OR product_type = '';

-- 订单项从商品冗余 product_type
UPDATE sales_order_item soi
    JOIN product p ON soi.product_id = p.id
    SET soi.product_type = p.product_type
    WHERE soi.product_type IS NULL;

-- 兜底：仍有 NULL 的订单项(历史脏数据/商品已删)按实物处理
UPDATE sales_order_item SET product_type = 'PHYSICAL'
    WHERE product_type IS NULL;
