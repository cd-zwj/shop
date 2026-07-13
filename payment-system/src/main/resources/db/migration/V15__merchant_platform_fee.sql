SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE merchant_balance ADD COLUMN total_platform_fee DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT ''累计平台服务费''',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'merchant_balance'
      AND column_name = 'total_platform_fee'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE merchant_wallet_log ADD COLUMN fee_amount DECIMAL(18, 2) NULL COMMENT ''平台服务费金额''',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'merchant_wallet_log'
      AND column_name = 'fee_amount'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
