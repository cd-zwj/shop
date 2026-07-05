ALTER TABLE order_delivery_record
    ADD COLUMN product_name VARCHAR(200) NULL COMMENT '商品名称快照' AFTER product_id;

UPDATE order_delivery_record odr
JOIN sales_order_item soi ON odr.order_item_id = soi.id
SET odr.product_name = soi.product_name
WHERE odr.product_name IS NULL;
