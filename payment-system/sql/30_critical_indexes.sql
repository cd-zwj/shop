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

-- member_points_log: 预扣查询覆盖索引
ALTER TABLE `member_points_log` ADD INDEX `idx_user_tenant_biz` (`platform_user_id`, `tenant_id`, `biz_type`, `biz_no`, `status`);

-- member_points_log: 积分过期扫描索引
ALTER TABLE `member_points_log` ADD INDEX `idx_points_expire_scan` (`status`, `expire_time`, `tenant_id`, `platform_user_id`);
