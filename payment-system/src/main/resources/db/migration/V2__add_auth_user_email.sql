-- V2: Add email column and unique index to auth_user (idempotent)
-- Compatible with MySQL 5.7+ using stored procedure for idempotent DDL

DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS add_email_column_if_needed()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'auth_user'
          AND COLUMN_NAME = 'email'
    ) THEN
        ALTER TABLE auth_user ADD COLUMN email VARCHAR(255) NULL COMMENT '邮箱' AFTER username;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'auth_user'
          AND INDEX_NAME = 'uk_auth_user_email'
    ) THEN
        ALTER TABLE auth_user ADD UNIQUE INDEX uk_auth_user_email (email);
    END IF;
END$$
DELIMITER ;

CALL add_email_column_if_needed();
DROP PROCEDURE add_email_column_if_needed;
