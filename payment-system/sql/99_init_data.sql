-- ========================================
-- 初始化数据
-- ========================================

USE `payment_db`;

-- 插入默认租户
INSERT INTO `tenant` (`tenant_code`, `name`, `contact`, `status`) VALUES
('TENANT_001', '默认商户', '管理员', 1);

-- 插入默认管理员用户（密码：admin123，需要在实际使用时修改）
INSERT INTO `sys_user` (`tenant_id`, `username`, `password`, `nickname`, `user_type`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJw2', '管理员', 2, 1);

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

-- 插入默认积分兑换商品
INSERT INTO `exchange_product` (`tenant_id`, `product_name`, `points_required`, `stock`, `exchange_limit`, `description`, `status`, `sort_order`) VALUES
(1, '10元优惠券', 1000, 100, 5, '满50元可用', 1, 1),
(1, '20元优惠券', 2000, 50, 3, '满100元可用', 1, 2),
(1, '精美礼品', 5000, 20, 1, '限量版礼品', 1, 3);

-- 为默认租户创建商家余额记录
INSERT INTO `merchant_balance` (`tenant_id`, `balance`, `frozen_balance`, `total_income`, `total_withdrawal`) VALUES
(1, 0.00, 0.00, 0.00, 0.00);
