-- The full SQL bootstrap may already have created these indexes before Flyway
-- establishes its history table, so each addition must be idempotent.

CREATE TABLE IF NOT EXISTS coupon_expire_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_coupon_id BIGINT NOT NULL,
    coupon_template_id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    platform_user_id BIGINT NOT NULL,
    biz_no VARCHAR(64) NULL,
    expire_reason VARCHAR(64) NULL,
    expire_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_coupon_expire_user_coupon (user_coupon_id),
    KEY idx_coupon_expire_tenant (tenant_id),
    KEY idx_coupon_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Coupon expiration record';

CREATE TABLE IF NOT EXISTS coupon_lock_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_coupon_id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    platform_user_id BIGINT NULL,
    order_id BIGINT NULL,
    order_no VARCHAR(64) NULL,
    biz_no VARCHAR(64) NULL,
    lock_time DATETIME NOT NULL,
    lock_status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    PRIMARY KEY (id),
    KEY idx_coupon_lock_user_coupon (user_coupon_id),
    KEY idx_coupon_lock_tenant (tenant_id),
    KEY idx_coupon_lock_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Coupon lock record';

CREATE TABLE IF NOT EXISTS coupon_receive_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_coupon_id BIGINT NOT NULL,
    coupon_template_id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    platform_user_id BIGINT NOT NULL,
    biz_no VARCHAR(64) NULL,
    receive_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_coupon_receive_user_coupon (user_coupon_id),
    KEY idx_coupon_receive_tenant (tenant_id),
    KEY idx_coupon_receive_user (platform_user_id, receive_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Coupon receive record';

CREATE TABLE IF NOT EXISTS coupon_release_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_coupon_id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    platform_user_id BIGINT NULL,
    order_id BIGINT NULL,
    order_no VARCHAR(64) NULL,
    biz_no VARCHAR(64) NULL,
    release_reason VARCHAR(64) NULL,
    release_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_coupon_release_user_coupon (user_coupon_id),
    KEY idx_coupon_release_tenant (tenant_id),
    KEY idx_coupon_release_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Coupon release record';

CREATE TABLE IF NOT EXISTS coupon_write_off_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_coupon_id BIGINT NOT NULL,
    coupon_template_id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    platform_user_id BIGINT NULL,
    order_id BIGINT NULL,
    order_no VARCHAR(64) NULL,
    biz_no VARCHAR(64) NULL,
    discount_amount DECIMAL(18, 2) NOT NULL,
    write_off_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_coupon_write_off_user_coupon (user_coupon_id),
    KEY idx_coupon_write_off_tenant (tenant_id),
    KEY idx_coupon_write_off_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Coupon write-off record';

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE coupon_lock_record ADD COLUMN platform_user_id BIGINT NULL AFTER tenant_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_lock_record'
      AND column_name = 'platform_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE coupon_release_record ADD COLUMN platform_user_id BIGINT NULL AFTER tenant_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_release_record'
      AND column_name = 'platform_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE coupon_write_off_record ADD COLUMN platform_user_id BIGINT NULL AFTER tenant_id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_write_off_record'
      AND column_name = 'platform_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE coupon_lock_record record
INNER JOIN user_coupon coupon ON coupon.id = record.user_coupon_id
SET record.platform_user_id = coupon.platform_user_id
WHERE record.platform_user_id IS NULL;

UPDATE coupon_release_record record
INNER JOIN user_coupon coupon ON coupon.id = record.user_coupon_id
SET record.platform_user_id = coupon.platform_user_id
WHERE record.platform_user_id IS NULL;

UPDATE coupon_write_off_record record
INNER JOIN user_coupon coupon ON coupon.id = record.user_coupon_id
SET record.platform_user_id = coupon.platform_user_id
WHERE record.platform_user_id IS NULL;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'merchant_wallet_log'
      AND index_name = 'idx_merchant_activity_page'
);
SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'merchant_wallet_log'
);
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE merchant_wallet_log ADD INDEX idx_merchant_activity_page (platform_user_id, create_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'member_points_log'
      AND index_name = 'idx_points_activity_page'
);
SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'member_points_log'
);
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE member_points_log ADD INDEX idx_points_activity_page (platform_user_id, create_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'member_growth_log'
      AND index_name = 'idx_growth_activity_page'
);
SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'member_growth_log'
);
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE member_growth_log ADD INDEX idx_growth_activity_page (platform_user_id, create_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_coupon'
      AND index_name = 'idx_user_coupon_activity_join'
);
SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'user_coupon'
);
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE user_coupon ADD INDEX idx_user_coupon_activity_join (platform_user_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_expire_record'
      AND index_name = 'idx_coupon_expire_activity_page'
);
SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_expire_record'
);
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE coupon_expire_record ADD INDEX idx_coupon_expire_activity_page (platform_user_id, expire_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_lock_record'
      AND index_name = 'idx_coupon_lock_activity_page'
);
SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_lock_record'
);
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE coupon_lock_record ADD INDEX idx_coupon_lock_activity_page (platform_user_id, lock_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_release_record'
      AND index_name = 'idx_coupon_release_activity_page'
);
SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_release_record'
);
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE coupon_release_record ADD INDEX idx_coupon_release_activity_page (platform_user_id, release_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_write_off_record'
      AND index_name = 'idx_coupon_write_off_activity_page'
);
SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'coupon_write_off_record'
);
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE coupon_write_off_record ADD INDEX idx_coupon_write_off_activity_page (platform_user_id, write_off_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
