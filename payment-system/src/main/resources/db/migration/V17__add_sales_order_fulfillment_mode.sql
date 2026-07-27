ALTER TABLE sales_order
    ADD COLUMN fulfillment_mode VARCHAR(32) NULL COMMENT '订单履约方式：STORE_PICKUP / EXPRESS_DELIVERY 等' AFTER store_id,
    ADD INDEX idx_sales_order_tenant_fulfillment_mode (tenant_id, fulfillment_mode);
