DROP PROCEDURE IF EXISTS migrate_legacy_product_stock;

DELIMITER $$
CREATE PROCEDURE migrate_legacy_product_stock()
BEGIN
    DECLARE legacy_row_count BIGINT DEFAULT 0;
    DECLARE migrated_row_count BIGINT DEFAULT 0;
    DECLARE legacy_quantity BIGINT DEFAULT 0;
    DECLARE migrated_quantity BIGINT DEFAULT 0;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'product_stock'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM product_stock legacy_stock
            LEFT JOIN product product_record ON product_record.id = legacy_stock.product_id
            WHERE product_record.id IS NULL
               OR product_record.tenant_id <> legacy_stock.tenant_id
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot migrate product_stock: product ownership mismatch';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM product_stock legacy_stock
            WHERE legacy_stock.quantity < 0
               OR legacy_stock.version < 0
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot migrate product_stock: negative quantity or version';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM (
                SELECT legacy_stock.tenant_id
                FROM product_stock legacy_stock
                JOIN store tenant_store
                  ON tenant_store.tenant_id = legacy_stock.tenant_id
                 AND tenant_store.deleted = 0
                GROUP BY legacy_stock.tenant_id
                HAVING COUNT(DISTINCT tenant_store.id) > 1
            ) ambiguous_store
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot migrate product_stock: tenant has multiple stores';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM product_stock legacy_stock
            JOIN store_product_stock target_stock
              ON target_stock.tenant_id = legacy_stock.tenant_id
             AND target_stock.product_id = legacy_stock.product_id
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot migrate product_stock: target stock already exists';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM product_stock legacy_stock
            JOIN store_product target_relation
              ON target_relation.tenant_id = legacy_stock.tenant_id
             AND target_relation.product_id = legacy_stock.product_id
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot migrate product_stock: target store product already exists';
        END IF;

        INSERT INTO store (store_no, tenant_id, store_name, store_type, status, deleted)
        SELECT CONCAT('MIGRATED-', legacy_stock.tenant_id),
               legacy_stock.tenant_id,
               '迁移默认门店',
               'DIRECT',
               1,
               0
        FROM product_stock legacy_stock
        LEFT JOIN store tenant_store
          ON tenant_store.tenant_id = legacy_stock.tenant_id
         AND tenant_store.deleted = 0
        GROUP BY legacy_stock.tenant_id
        HAVING COUNT(tenant_store.id) = 0;

        INSERT INTO store_product
            (tenant_id, store_id, product_id, price, status)
        SELECT legacy_stock.tenant_id,
               tenant_store.id,
               legacy_stock.product_id,
               product_record.price,
               product_record.status
        FROM product_stock legacy_stock
        JOIN product product_record ON product_record.id = legacy_stock.product_id
        JOIN store tenant_store
          ON tenant_store.tenant_id = legacy_stock.tenant_id
         AND tenant_store.deleted = 0;

        INSERT INTO store_product_stock
            (tenant_id, store_id, product_id, quantity, locked_quantity, version)
        SELECT legacy_stock.tenant_id,
               tenant_store.id,
               legacy_stock.product_id,
               legacy_stock.quantity,
               0,
               legacy_stock.version
        FROM product_stock legacy_stock
        JOIN store tenant_store
          ON tenant_store.tenant_id = legacy_stock.tenant_id
         AND tenant_store.deleted = 0;

        SELECT COUNT(*), COALESCE(SUM(quantity), 0)
        INTO legacy_row_count, legacy_quantity
        FROM product_stock;

        SELECT COUNT(*), COALESCE(SUM(target_stock.quantity), 0)
        INTO migrated_row_count, migrated_quantity
        FROM product_stock legacy_stock
        JOIN store_product_stock target_stock
          ON target_stock.tenant_id = legacy_stock.tenant_id
         AND target_stock.product_id = legacy_stock.product_id;

        IF migrated_row_count <> legacy_row_count
           OR migrated_quantity <> legacy_quantity THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot drop product_stock: backfill verification failed';
        END IF;

        DROP TABLE product_stock;
    END IF;
END$$
DELIMITER ;

CALL migrate_legacy_product_stock();
DROP PROCEDURE migrate_legacy_product_stock;

SET @product_store_id_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'product'
      AND column_name = 'store_id'
);
SET @drop_product_store_id_sql = IF(
    @product_store_id_exists > 0,
    'ALTER TABLE product DROP COLUMN store_id',
    'SELECT 1'
);
PREPARE drop_product_store_id_stmt FROM @drop_product_store_id_sql;
EXECUTE drop_product_store_id_stmt;
DEALLOCATE PREPARE drop_product_store_id_stmt;

SET @product_fulfillment_mode_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'product'
      AND column_name = 'fulfillment_mode'
);
SET @drop_product_fulfillment_mode_sql = IF(
    @product_fulfillment_mode_exists > 0,
    'ALTER TABLE product DROP COLUMN fulfillment_mode',
    'SELECT 1'
);
PREPARE drop_product_fulfillment_mode_stmt FROM @drop_product_fulfillment_mode_sql;
EXECUTE drop_product_fulfillment_mode_stmt;
DEALLOCATE PREPARE drop_product_fulfillment_mode_stmt;
