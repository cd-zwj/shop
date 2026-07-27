USE `payment_db`;

SET @schema_name = DATABASE();
SET @email_verified_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'platform_user'
      AND column_name = 'email_verified'
);
SET @email_verified_sql = IF(
    @email_verified_exists = 0,
    'ALTER TABLE `platform_user` ADD COLUMN `email_verified` TINYINT(1) NOT NULL DEFAULT ''0'' COMMENT ''邮箱是否已验证'' AFTER `email`',
    'SELECT 1'
);
PREPARE stmt FROM @email_verified_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @email_index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'platform_user'
      AND index_name = 'uk_email'
);
SET @email_index_sql = IF(
    @email_index_exists = 0,
    'ALTER TABLE `platform_user` ADD UNIQUE KEY `uk_email` (`email`)',
    'SELECT 1'
);
PREPARE stmt FROM @email_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
