-- =============================================
-- AI 场景化工具权限码
-- 为三端角色分配 AI 工具访问权限，
-- 使 ScenarioToolExposureService 能正确暴露业务工具。
-- =============================================

-- 插入 AI 工具权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('ai:user:wallet', 'AI 钱包工具', 'ai', 'AI 助手可读取当前用户钱包、积分、优惠券数据'),
('ai:user:orders', 'AI 订单工具', 'ai', 'AI 助手可读取当前用户订单、退款、通知数据'),
('ai:merchant:orders', 'AI 商家订单工具', 'ai', 'AI 助手可读取当前商家租户订单、退款数据'),
('ai:merchant:marketing', 'AI 营销工具', 'ai', 'AI 助手可读取当前商家租户优惠券、活动、会员数据'),
('ai:merchant:finance', 'AI 财务工具', 'ai', 'AI 助手可读取当前商家租户钱包、提现数据'),
('ai:admin:governance', 'AI 治理工具', 'ai', 'AI 助手可读取平台商户、用户、交易、权限数据'),
('ai:admin:risk', 'AI 风控工具', 'ai', 'AI 助手可读取平台风控、退款、提现异常数据');

-- 分配给用户角色 (role_id = 1)
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission_code IN (
    'ai:user:wallet', 'ai:user:orders'
);

-- 分配给商家角色 (role_id = 2)
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code IN (
    'ai:merchant:orders', 'ai:merchant:marketing', 'ai:merchant:finance'
);

-- 管理员角色 (role_id = 3) 拥有所有权限，但确保 AI 权限也被覆盖
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE permission_code IN (
    'ai:admin:governance', 'ai:admin:risk'
);
