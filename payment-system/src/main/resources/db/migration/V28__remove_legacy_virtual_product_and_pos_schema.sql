-- Preserve historical migration files for checksum validation, then remove
-- virtual-product and POS artifacts through a forward-only migration.

DELETE role_permission
FROM sys_role_permission role_permission
JOIN sys_permission permission ON permission.id = role_permission.permission_id
WHERE permission.permission_code = 'pos:checkout';

DELETE FROM sys_permission
WHERE permission_code = 'pos:checkout';

ALTER TABLE product
    DROP INDEX idx_product_virtual_type,
    DROP COLUMN virtual_type_id,
    DROP COLUMN virtual_category_id;

DROP TABLE IF EXISTS virtual_product_category;
DROP TABLE IF EXISTS virtual_product_type;

ALTER TABLE sales_order
    DROP INDEX idx_sales_order_cashier_time,
    DROP COLUMN cashier_id,
    DROP COLUMN payment_method,
    DROP COLUMN received_amount,
    DROP COLUMN change_amount;
