-- ========================================
-- 初始化数据
-- ========================================

USE `payment_db`;

-- 插入默认租户
INSERT INTO `tenant` (`tenant_code`, `name`, `contact`, `status`) VALUES
('TENANT_001', '默认商户', '管理员', 1)
ON DUPLICATE KEY UPDATE
`name` = VALUES(`name`),
`contact` = VALUES(`contact`),
`status` = VALUES(`status`);

-- 插入默认管理员用户（密码：admin123，需要在实际使用时修改）
INSERT INTO `sys_user` (`tenant_id`, `username`, `password`, `nickname`, `user_type`, `status`) VALUES
((SELECT `id` FROM `tenant` WHERE `tenant_code` = 'TENANT_001' LIMIT 1), 'admin', '$2a$10$RM41BiXeQbfAcRJbjh2g2O6Z9lHJEO/Or6b40U8p.BXHmbSwfs5Ky', '管理员', 2, 1)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`nickname` = VALUES(`nickname`),
`user_type` = 2,
`status` = 1,
`deleted` = 0;


-- 插入默认平台管理员账号（密码：admin123，需要在实际使用时修改）
INSERT INTO `platform_user` (`user_no`, `username`, `phone`, `email`, `password_hash`, `status`, `deleted`) VALUES
('PU202606070001', 'admin', '13800000001', 'admin@test.com', '$2a$10$RM41BiXeQbfAcRJbjh2g2O6Z9lHJEO/Or6b40U8p.BXHmbSwfs5Ky', 1, 0)
ON DUPLICATE KEY UPDATE
`phone` = VALUES(`phone`),
`email` = VALUES(`email`),
`status` = 1,
`deleted` = 0;

-- 管理端 RBAC 授权绑定 platform_user.id，而不是 sys_user.id
INSERT INTO `sys_user_role` (`principal_type`, `user_id`, `role_id`)
SELECT 'admin', pu.`id`, r.`id`
FROM `platform_user` pu
JOIN `sys_role` r ON r.`role_code` = 'admin'
WHERE pu.`username` = 'admin'
  AND pu.`deleted` = 0
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);
-- 插入默认充值规则
INSERT INTO `recharge_rule` (`tenant_id`, `recharge_amount`, `gift_amount`, `gift_points`, `status`, `sort_order`) VALUES
(1, 100.00, 0.00, 100, 1, 1),
(1, 200.00, 20.00, 250, 1, 2),
(1, 500.00, 60.00, 700, 1, 3),
(1, 1000.00, 150.00, 1500, 1, 4);

-- 插入默认积分规则
INSERT INTO `points_rule` (`tenant_id`, `rule_name`, `rule_type`, `points_amount`, `condition_amount`, `status`) VALUES
(1, '支付获得积分', 'PAYMENT', 1, 1.00, 1),
(1, '每日签到', 'SIGNIN', 10, NULL, 1),
(1, '分享好友', 'SHARE', 20, NULL, 1);

-- 为默认租户创建商家余额记录
INSERT INTO `merchant_balance` (`tenant_id`, `balance`, `frozen_balance`, `total_income`, `total_withdrawal`) VALUES
(1, 0.00, 0.00, 0.00, 0.00);
