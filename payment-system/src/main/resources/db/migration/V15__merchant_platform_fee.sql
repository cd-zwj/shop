ALTER TABLE merchant_balance
  ADD COLUMN IF NOT EXISTS total_platform_fee DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '累计平台服务费';

ALTER TABLE merchant_wallet_log
  ADD COLUMN IF NOT EXISTS fee_amount DECIMAL(18, 2) NULL COMMENT '平台服务费金额';
