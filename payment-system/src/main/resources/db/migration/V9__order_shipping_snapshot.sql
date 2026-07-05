ALTER TABLE sales_order
    ADD COLUMN shipping_address_id BIGINT NULL COMMENT '下单时选择的收货地址ID' AFTER source,
    ADD COLUMN shipping_receiver_name VARCHAR(50) NULL COMMENT '收货人快照' AFTER shipping_address_id,
    ADD COLUMN shipping_phone VARCHAR(20) NULL COMMENT '收货手机号快照' AFTER shipping_receiver_name,
    ADD COLUMN shipping_province VARCHAR(50) NULL COMMENT '收货省份快照' AFTER shipping_phone,
    ADD COLUMN shipping_city VARCHAR(50) NULL COMMENT '收货城市快照' AFTER shipping_province,
    ADD COLUMN shipping_district VARCHAR(50) NULL COMMENT '收货区县快照' AFTER shipping_city,
    ADD COLUMN shipping_detail VARCHAR(255) NULL COMMENT '收货详细地址快照' AFTER shipping_district,
    ADD INDEX idx_sales_order_shipping_address (shipping_address_id);
