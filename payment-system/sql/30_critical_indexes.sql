-- ========================================
-- 关键索引补全
-- ========================================

USE `payment_db`;

-- sales_order: pay_status 索引（管理端按状态筛选高频查询）
ALTER TABLE `sales_order` ADD INDEX `idx_pay_status` (`pay_status`);

-- payment_bill: pay_status + create_time 复合索引（交易监控查询）
ALTER TABLE `payment_bill` ADD INDEX `idx_pay_status_time` (`pay_status`, `create_time`);

-- withdrawal: tenant_id + status + apply_time 复合索引（提现列表查询）
ALTER TABLE `withdrawal` ADD INDEX `idx_tenant_status_time` (`tenant_id`, `status`, `apply_time`);
