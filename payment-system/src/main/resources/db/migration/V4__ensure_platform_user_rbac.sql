-- Ensure platform users logged in as platform:<id> have matching RBAC rows.

INSERT INTO sys_role (role_code, role_name, description)
VALUES
    ('user', '普通用户', '普通用户，可以下单、支付、查看订单等'),
    ('merchant', '商家', '商家用户，可以管理商品、查看销售数据、提现等'),
    ('admin', '管理员', '平台管理员，可以管理商家、审核提现等')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    status = 1;

INSERT INTO sys_permission (permission_code, permission_name, module, description)
VALUES
    ('merchant:product:read', '商户商品查看', 'product', '商户端查看商品列表和详情'),
    ('merchant:product:write', '商户商品维护', 'product', '商户端创建、更新、上下架商品'),
    ('merchant:store:read', '商户门店查看', 'store', '商户端查看门店主数据'),
    ('merchant:store:write', '商户门店维护', 'store', '商户端维护门店主数据'),
    ('merchant:refund:list', '商户退款列表', 'refund', '商户端查看退款申请列表'),
    ('merchant:refund:audit', '商户退款审核', 'refund', '商户端审核退款申请'),
    ('merchant:withdrawal:view', '商户提现余额查看', 'withdrawal', '商户端查看提现余额'),
    ('merchant:withdrawal:list', '商户提现记录', 'withdrawal', '商户端查看提现记录'),
    ('merchant:withdrawal:create', '商户提现申请', 'withdrawal', '商户端创建提现申请'),
    ('statistics:view', '查看统计', 'statistics', '查看销售统计数据'),
    ('statistics:export', '导出统计', 'statistics', '导出销售统计数据'),
    ('file:list', '文件列表', 'file', '查看上传文件列表'),
    ('admin:dashboard', '管理后台概览', 'admin', '管理后台数据概览'),
    ('admin:user:list', '平台用户列表', 'admin', '查看平台用户列表与详情'),
    ('admin:user:update', '平台用户状态维护', 'admin', '启用或禁用平台用户'),
    ('admin:permission:list', '权限列表查询', 'admin', '查询系统所有权限'),
    ('admin:user:permission', '用户权限管理', 'admin', '管理用户额外权限'),
    ('admin:trade:overview', '交易总览', 'admin', '查看平台交易聚合数据'),
    ('admin:trade:list', '交易列表', 'admin', '查看订单、支付单、充值单列表'),
    ('admin:trade:detail', '交易详情', 'admin', '查看订单详情'),
    ('admin:category:list', '商品分类列表', 'admin', '查看商品分类'),
    ('admin:category:create', '创建商品分类', 'admin', '创建商品分类'),
    ('admin:category:update', '更新商品分类', 'admin', '更新商品分类'),
    ('admin:category:delete', '删除商品分类', 'admin', '删除商品分类'),
    ('admin:merchant:list', '商家列表', 'admin', '查看商家列表'),
    ('admin:merchant:detail', '商家详情', 'admin', '查看商家详情'),
    ('admin:merchant:create', '创建商家', 'admin', '创建新商家'),
    ('admin:merchant:update', '更新商家', 'admin', '更新商家信息'),
    ('admin:merchant:enable', '启用商家', 'admin', '启用商家'),
    ('admin:merchant:disable', '禁用商家', 'admin', '禁用商家'),
    ('admin:merchant:balance', '商家余额', 'admin', '查看商家余额'),
    ('admin:marketing:list', '营销活动列表', 'admin', '查看营销活动列表'),
    ('admin:marketing:create', '创建营销活动', 'admin', '创建营销活动'),
    ('admin:marketing:update', '更新营销活动', 'admin', '更新营销活动'),
    ('admin:withdrawal:list', '提现列表', 'admin', '查看提现申请列表'),
    ('admin:withdrawal:approve', '审核通过提现', 'admin', '审核通过提现申请'),
    ('admin:withdrawal:reject', '拒绝提现', 'admin', '拒绝提现申请'),
    ('admin:compensation:list', '补偿任务查询', 'admin', '查看补偿任务和重试任务列表'),
    ('admin:compensation:operate', '补偿任务操作', 'admin', '重试或取消补偿任务和重试任务'),
    ('admin:auditlog:list', '审计日志列表', 'admin', '查看审计日志'),
    ('admin:behaviorlog:list', '行为日志列表', 'admin', '查看行为日志'),
    ('admin:auth-provider:list', '认证源列表', 'admin', '查看认证源列表'),
    ('admin:auth-provider:detail', '认证源详情', 'admin', '查看认证源详情'),
    ('admin:auth-provider:create', '创建认证源', 'admin', '创建认证源'),
    ('admin:auth-provider:update', '更新认证源', 'admin', '更新认证源'),
    ('admin:auth-provider:enable', '启用认证源', 'admin', '启用认证源'),
    ('admin:auth-provider:disable', '禁用认证源', 'admin', '禁用认证源'),
    ('user:info', '查看个人信息', 'user', '获取当前登录用户信息'),
    ('user:logout', '退出登录', 'user', '用户退出登录')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    module = VALUES(module),
    description = VALUES(description);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'merchant:product:read',
    'merchant:product:write',
    'merchant:store:read',
    'merchant:store:write',
    'merchant:refund:list',
    'merchant:refund:audit',
    'merchant:withdrawal:view',
    'merchant:withdrawal:list',
    'merchant:withdrawal:create',
    'statistics:view',
    'statistics:export',
    'file:list'
)
WHERE r.role_code = 'merchant'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('user:info', 'user:logout')
WHERE r.role_code = 'user'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'admin'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_user_role (user_id, role_id)
SELECT pu.id, r.id
FROM platform_user pu
JOIN sys_role r ON r.role_code = 'admin'
WHERE pu.username = 'admin'
  AND pu.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_user_role (user_id, role_id)
SELECT pu.id, r.id
FROM platform_user pu
JOIN sys_role r ON r.role_code = 'merchant'
WHERE pu.username = 'merchant'
  AND pu.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_user_role (user_id, role_id)
SELECT pu.id, r.id
FROM platform_user pu
JOIN sys_role r ON r.role_code = 'user'
WHERE pu.username IN ('user', 'user2')
  AND pu.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
