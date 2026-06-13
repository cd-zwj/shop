-- ========================================
-- 权限与风控补充脚本
-- 新增商户端权限码、文件列表权限、商家角色绑定、无限流资金接口说明
-- ========================================

USE `payment_db`;

-- 1. 补充商户端退款、提现、文件列表权限（幂等）
INSERT IGNORE INTO sys_permission (permission_code, permission_name, module, description) VALUES
('merchant:refund:list', '商户退款列表', 'refund', '商户端查看退款申请列表'),
('merchant:refund:audit', '商户退款审核', 'refund', '商户端审核退款申请'),
('merchant:withdrawal:view', '商户提现余额查看', 'withdrawal', '商户端查看提现余额'),
('merchant:withdrawal:list', '商户提现记录', 'withdrawal', '商户端查看提现记录'),
('merchant:withdrawal:create', '商户提现申请', 'withdrawal', '商户端创建提现申请'),
('file:list', '文件列表查询', 'file', '查询文件资产列表');

-- 2. 给商家角色授予退款和提现权限（幂等）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code IN (
    'merchant:refund:list', 'merchant:refund:audit',
    'merchant:withdrawal:view', 'merchant:withdrawal:list', 'merchant:withdrawal:create',
    'file:list'
);

-- 3. 管理员角色默认已有全部权限，这里再次确认关键权限（幂等）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE permission_code IN (
    'merchant:refund:list', 'merchant:refund:audit',
    'merchant:withdrawal:view', 'merchant:withdrawal:list', 'merchant:withdrawal:create',
    'file:list'
);

-- 4. 记录当前已完成的接口限流补充：
--    - /v1/app/wallets/unified/recharges 60s/次
--    - /v1/app/tenants/{tenantId}/wallet/recharges 60s/次
--    - /v1/app/tenants/{tenantId}/refunds 300s/次
--    如需继续扩展，可在此脚本中追加审计记录表说明。
