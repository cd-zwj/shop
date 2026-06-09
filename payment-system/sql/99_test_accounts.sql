-- ========================================
-- 测试账号初始化数据
-- 使用前确保 99_init_data.sql 已执行
-- ========================================

USE `payment_db`;

-- ========== 平台用户（V1 新模型） ==========

-- 管理员用户
INSERT INTO `platform_user` (`user_no`, `username`, `phone`, `email`, `password_hash`, `status`, `deleted`) VALUES
('PU202606070001', 'admin', '13800000001', 'admin@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJw2', 1, 0);

-- 商户用户
INSERT INTO `platform_user` (`user_no`, `username`, `phone`, `email`, `password_hash`, `status`, `deleted`) VALUES
('PU202606070002', 'merchant', '13800000002', 'merchant@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJw2', 1, 0);

-- 普通用户
INSERT INTO `platform_user` (`user_no`, `username`, `phone`, `email`, `password_hash`, `status`, `deleted`) VALUES
('PU202606070003', 'user', '13800000003', 'user@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJw2', 1, 0);

-- 第二个普通用户（用于测试越权）
INSERT INTO `platform_user` (`user_no`, `username`, `phone`, `email`, `password_hash`, `status`, `deleted`) VALUES
('PU202606070004', 'user2', '13800000004', 'user2@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJw2', 1, 0);

-- ========== 商户员工关系 ==========

-- merchant 用户作为 tenant 1 的员工
INSERT INTO `tenant_employee` (`tenant_id`, `platform_user_id`, `employee_no`, `employee_role`, `status`, `deleted`) VALUES
(1, 2, 'EMP001', 'OWNER', 1, 0);

-- ========== 商户会员关系 ==========

-- user 和 user2 作为 tenant 1 的会员
INSERT INTO `tenant_member` (`tenant_id`, `platform_user_id`, `member_no`, `member_status`, `register_source`, `deleted`) VALUES
(1, 3, 'MEM001', 'ACTIVE', 'APP', 0),
(1, 4, 'MEM002', 'ACTIVE', 'APP', 0);

-- ========== RBAC 角色分配 ==========

-- 给 platform_user 分配角色（如果 RBAC 用 platform_user_id）
-- admin -> 角色 3 (admin)
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 3)
ON DUPLICATE KEY UPDATE `role_id` = 3;

-- ========== 测试商品 ==========

INSERT INTO `product` (`tenant_id`, `product_code`, `name`, `description`, `price`, `category`, `image_url`, `status`, `deleted`) VALUES
(1, 'PROD001', '精品咖啡', '手冲精品咖啡，阿拉比卡豆', 28.00, '餐饮', NULL, 1, 0),
(1, 'PROD002', '芝士蛋糕', '日式轻芝士蛋糕', 38.00, '餐饮', NULL, 1, 0),
(1, 'PROD003', '鲜榨果汁', '当季鲜榨混合果汁', 18.00, '餐饮', NULL, 1, 0),
(1, 'PROD004', '蓝牙耳机', '降噪蓝牙耳机', 299.00, '数码', NULL, 1, 0),
(1, 'PROD005', '充电宝', '20000mAh 大容量充电宝', 89.00, '数码', NULL, 1, 0);

-- ========== 商品库存 ==========

INSERT INTO `product_stock` (`tenant_id`, `product_id`, `quantity`, `version`, `update_time`) VALUES
(1, 1, 100, 0, NOW()),
(1, 2, 50, 0, NOW()),
(1, 3, 200, 0, NOW()),
(1, 4, 30, 0, NOW()),
(1, 5, 80, 0, NOW());

-- ========== 测试优惠券模板 ==========

INSERT INTO `coupon_template` (`template_no`, `tenant_id`, `template_scope`, `template_name`, `coupon_type`, `threshold_amount`, `discount_amount`, `total_quantity`, `per_user_limit`, `can_stack_balance`, `can_stack_points`, `can_stack_other_coupon`, `applicable_product_scope`, `status`) VALUES
('TPL001', 1, 'TENANT', '新人专享券', 'FULL_REDUCTION', 50.00, 10.00, 100, 1, 0, 0, 0, 'ALL', 'ACTIVE'),
('TPL002', 1, 'TENANT', '满100打8折', 'DISCOUNT', 100.00, NULL, 50, 2, 1, 1, 0, 'ALL', 'ACTIVE'),
('TPL003', 1, 'TENANT', '无门槛5元券', 'CASH', 0.00, 5.00, 200, 3, 0, 0, 0, 'ALL', 'ACTIVE');

-- ========== 测试订单 ==========

INSERT INTO `sales_order` (`order_no`, `tenant_id`, `platform_user_id`, `order_status`, `pay_status`, `total_amount`, `discount_amount`, `payable_amount`, `wallet_strategy`, `subject`, `source`, `deleted`) VALUES
('SO20260607001', 1, 3, 'COMPLETED', 'SUCCESS', 66.00, 0.00, 66.00, 'NO_WALLET', '精品咖啡 x1 + 芝士蛋糕 x1', 'APP_CART', 0),
('SO20260607002', 1, 3, 'PENDING', 'UNPAID', 299.00, 0.00, 299.00, 'NO_WALLET', '蓝牙耳机 x1', 'APP_CART', 0);

INSERT INTO `sales_order_item` (`order_id`, `order_no`, `tenant_id`, `product_id`, `product_name`, `price`, `quantity`, `subtotal`) VALUES
(1, 'SO20260607001', 1, 1, '精品咖啡', 28.00, 1, 28.00),
(1, 'SO20260607001', 1, 2, '芝士蛋糕', 38.00, 1, 38.00),
(2, 'SO20260607002', 1, 4, '蓝牙耳机', 299.00, 1, 299.00);

-- ========== 会员积分账户 ==========

INSERT INTO `member_points_account` (`tenant_id`, `platform_user_id`, `points`, `total_earned`, `total_used`, `version`, `status`) VALUES
(1, 3, 500, 500, 0, 0, 'ACTIVE'),
(1, 4, 100, 100, 0, 0, 'ACTIVE');

-- ========== 会员等级 ==========

INSERT INTO `member_level` (`tenant_id`, `level_no`, `level_rank`, `level_name`, `upgrade_growth`, `discount_rate`, `benefit_json`, `status`) VALUES
(1, 'LV001', 1, '普通会员', 0, 1.00, NULL, 1),
(1, 'LV002', 2, '银卡会员', 500, 0.95, '{"freeShipping": false}', 1),
(1, 'LV003', 3, '金卡会员', 2000, 0.90, '{"freeShipping": true}', 1),
(1, 'LV004', 4, '钻石会员', 10000, 0.85, '{"freeShipping": true, "priority": true}', 1);
