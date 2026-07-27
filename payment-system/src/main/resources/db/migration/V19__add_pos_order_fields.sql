ALTER TABLE sales_order
    ADD COLUMN cashier_id BIGINT NULL COMMENT 'POS 收银员平台用户ID' AFTER store_id,
    ADD COLUMN payment_method VARCHAR(32) NULL COMMENT 'CASH/CARD/ONLINE_SCAN/WALLET' AFTER fulfillment_mode,
    ADD COLUMN received_amount DECIMAL(18,2) NULL COMMENT 'POS 实收金额' AFTER external_pay_amount,
    ADD COLUMN change_amount DECIMAL(18,2) NULL COMMENT 'POS 找零金额' AFTER received_amount,
    ADD INDEX idx_sales_order_tenant_store_source_time (tenant_id, store_id, source, create_time),
    ADD INDEX idx_sales_order_cashier_time (cashier_id, create_time);
