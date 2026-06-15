USE `payment_db`;

ALTER TABLE `payment_bill`
ADD COLUMN `status_remark` VARCHAR(255) DEFAULT NULL COMMENT '状态备注，建议与支付状态原因枚举保持一致'
AFTER `callback_status`;
