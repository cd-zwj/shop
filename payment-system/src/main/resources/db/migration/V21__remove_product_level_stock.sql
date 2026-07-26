DROP TABLE IF EXISTS product_stock;

ALTER TABLE product
    DROP COLUMN IF EXISTS store_id,
    DROP COLUMN IF EXISTS fulfillment_mode;
