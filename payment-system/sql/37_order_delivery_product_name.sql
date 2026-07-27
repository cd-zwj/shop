-- ========================================
-- 订单交付记录商品名称快照
-- ========================================

USE `payment_db`;

SET @order_delivery_product_name_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'order_delivery_record'
      AND column_name = 'product_name'
);
SET @order_delivery_product_name_sql = IF(
    @order_delivery_product_name_exists = 0,
    'ALTER TABLE `order_delivery_record` ADD COLUMN `product_name` VARCHAR(200) DEFAULT NULL COMMENT ''商品名称快照'' AFTER `product_id`',
    'SELECT 1'
);
PREPARE order_delivery_product_name_stmt FROM @order_delivery_product_name_sql;
EXECUTE order_delivery_product_name_stmt;
DEALLOCATE PREPARE order_delivery_product_name_stmt;

UPDATE `order_delivery_record` odr
JOIN `sales_order_item` soi ON odr.order_item_id = soi.id
SET odr.product_name = soi.product_name
WHERE odr.product_name IS NULL;
