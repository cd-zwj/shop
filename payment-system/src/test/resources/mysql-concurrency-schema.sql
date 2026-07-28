CREATE TABLE sales_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    platform_user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    pay_status VARCHAR(32) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    wallet_deduct_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    points_deduct_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    unified_wallet_deduct_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    merchant_wallet_deduct_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    external_pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    payable_amount DECIMAL(18,2) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    source VARCHAR(32),
    shipping_address_id BIGINT,
    shipping_receiver_name VARCHAR(50),
    shipping_phone VARCHAR(20),
    shipping_province VARCHAR(50),
    shipping_city VARCHAR(50),
    shipping_district VARCHAR(50),
    shipping_detail VARCHAR(255),
    wallet_strategy VARCHAR(32) NOT NULL,
    expire_time DATETIME,
    store_id BIGINT,
    fulfillment_mode VARCHAR(32),
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no_v1 (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sales_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200),
    price DECIMAL(18,2),
    quantity INT,
    subtotal DECIMAL(18,2),
    delivery_status VARCHAR(32),
    delivered_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refund_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    refund_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    order_item_id BIGINT,
    platform_user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    refund_type VARCHAR(20) NOT NULL,
    refund_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    refund_amount DECIMAL(18,2) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    evidence_urls_json TEXT,
    reject_reason VARCHAR(255),
    admin_id BIGINT,
    audit_time DATETIME,
    complete_time DATETIME,
    active_whole_order_key VARCHAR(64) GENERATED ALWAYS AS (
        CASE WHEN refund_status IN ('PENDING', 'APPROVED', 'PROCESSING') AND order_item_id IS NULL
             THEN order_no ELSE NULL END) VIRTUAL,
    active_order_item_key BIGINT GENERATED ALWAYS AS (
        CASE WHEN refund_status IN ('PENDING', 'APPROVED', 'PROCESSING') AND order_item_id IS NOT NULL
             THEN order_item_id ELSE NULL END) VIRTUAL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refund_application_no (refund_no),
    UNIQUE KEY uk_refund_application_active_whole (tenant_id, active_whole_order_key),
    UNIQUE KEY uk_refund_application_active_item (tenant_id, active_order_item_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE after_sale_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    refund_application_id BIGINT NOT NULL,
    refund_no VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    operator_role VARCHAR(16) NOT NULL,
    operator_id BIGINT,
    remark VARCHAR(1000),
    evidence_urls_json TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE store_product_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    locked_quantity INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_product_stock (store_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE store_inventory_change_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    change_quantity INT NOT NULL,
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,
    locked_before INT NOT NULL,
    locked_after INT NOT NULL,
    biz_type VARCHAR(32),
    biz_no VARCHAR(64),
    operator_id BIGINT,
    remark VARCHAR(255),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_inventory_biz_change
        (store_id, product_id, change_type, biz_type, biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE message_idempotent (
    message_id VARCHAR(100) PRIMARY KEY,
    queue_name VARCHAR(100) NOT NULL,
    message_body TEXT,
    consumer_name VARCHAR(100),
    status TINYINT NOT NULL DEFAULT 1,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_message_idempotent_queue (queue_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
