-- =============================================
-- RBAC 权限管理表
-- =============================================

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码: user, merchant, admin',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) COMMENT '角色描述',
    status INT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    module VARCHAR(50) COMMENT '所属模块',
    description VARCHAR(255) COMMENT '权限描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    principal_type VARCHAR(32) NOT NULL DEFAULT 'platform' COMMENT '账号体系: admin, merchant, platform',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (principal_type, user_id, role_id),
    KEY idx_user_id (principal_type, user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- =============================================
-- 初始化角色数据
-- =============================================
INSERT INTO sys_role (role_code, role_name, description) VALUES
('user', '普通用户', '普通用户，可以下单、支付、查看订单等'),
('merchant', '商家', '商家用户，可以管理商品、查看销售数据、提现等'),
('admin', '管理员', '平台管理员，可以管理商家、审核提现等');

-- =============================================
-- 初始化权限数据
-- =============================================

-- 用户端权限 (user模块)
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('user:info', '查看个人信息', 'user', '获取当前登录用户信息'),
('user:logout', '退出登录', 'user', '用户退出登录');

-- 订单权限 (order模块) - 用户端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('order:create', '创建订单', 'order', '用户创建订单'),
('order:pay', '支付订单', 'order', '用户支付订单'),
('order:query', '查询订单', 'order', '查询订单详情'),
('order:cancel', '取消订单', 'order', '取消订单');

-- 积分权限 (points模块) - 用户端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('points:view', '查看积分', 'points', '查看积分余额和记录');

-- 充值权限 (recharge模块) - 用户端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('recharge:create', '创建充值', 'recharge', '创建充值订单'),
('recharge:view', '查看充值记录', 'recharge', '查看充值记录');

-- 小程序权限 (miniprogram模块) - 用户端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('miniprogram:bindPhone', '绑定手机号', 'miniprogram', '小程序绑定手机号'),
('miniprogram:bindWechat', '微信登录', 'miniprogram', '小程序微信登录');

-- 商品权限 (product模块) - 商家端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('product:create', '创建商品', 'product', '商家创建商品'),
('product:update', '更新商品', 'product', '商家更新商品'),
('product:delete', '删除商品', 'product', '商家删除商品'),
('product:view', '查看商品', 'product', '查看商品详情'),
('product:list', '商品列表', 'product', '查看商品列表'),
('product:search', '搜索商品', 'product', '搜索商品');

-- 提现权限 (withdrawal模块) - 商家端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('withdrawal:create', '申请提现', 'withdrawal', '商家申请提现'),
('withdrawal:view', '查看提现记录', 'withdrawal', '查看提现记录');

-- 数据分析权限 (analysis模块) - 商家端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('analysis:sales', '销售分析', 'analysis', '查看销售数据分析'),
('analysis:dashboard', '数据概览', 'analysis', '商家数据概览');

-- 销售统计权限 (statistics模块) - 商家端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('statistics:view', '查看统计', 'statistics', '查看销售统计数据');

-- 商家信息权限 (merchant模块) - 商家端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('merchant:info', '查看商家信息', 'merchant', '查看自己的商家信息');

-- 管理端权限 (admin模块)
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('admin:dashboard', '管理后台概览', 'admin', '管理后台数据概览'),
('admin:merchant:list', '商家列表', 'admin', '查看商家列表'),
('admin:merchant:detail', '商家详情', 'admin', '查看商家详情'),
('admin:merchant:create', '创建商家', 'admin', '创建新商家'),
('admin:merchant:update', '更新商家', 'admin', '更新商家信息'),
('admin:merchant:enable', '启用商家', 'admin', '启用商家'),
('admin:merchant:disable', '禁用商家', 'admin', '禁用商家'),
('admin:withdrawal:list', '提现列表', 'admin', '查看提现申请列表'),
('admin:withdrawal:approve', '审核通过提现', 'admin', '审核通过提现申请'),
('admin:withdrawal:reject', '拒绝提现', 'admin', '拒绝提现申请');

-- 营销管理权限 (marketing模块) - 管理端
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('admin:marketing:list', '营销活动列表', 'admin', '查看营销活动列表'),
('admin:marketing:create', '创建营销活动', 'admin', '创建营销活动'),
('admin:marketing:update', '更新营销活动', 'admin', '更新营销活动');

-- AI功能权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('ai:chat', 'AI对话', 'ai', 'AI智能对话功能'),
('ai:analysis', 'AI分析', 'ai', 'AI数据分析功能');

-- 文件上传权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('file:upload', '文件上传', 'file', '上传文件');

-- =============================================
-- 分配角色权限
-- =============================================

-- 用户角色权限 (role_id = 1)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission_code IN (
    'user:info', 'user:logout',
    'order:create', 'order:pay', 'order:query', 'order:cancel',
    'points:view',
    'recharge:create', 'recharge:view',
    'miniprogram:bindPhone', 'miniprogram:bindWechat',
    'product:view', 'product:list', 'product:search',
    'ai:chat',
    'file:upload'
);

-- 商家角色权限 (role_id = 2)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code IN (
    'user:info', 'user:logout',
    'product:create', 'product:update', 'product:delete', 'product:view', 'product:list', 'product:search',
    'order:query',
    'withdrawal:create', 'withdrawal:view',
    'analysis:sales', 'analysis:dashboard',
    'statistics:view',
    'merchant:info',
    'ai:chat', 'ai:analysis',
    'file:upload'
);

-- 管理员角色权限 (role_id = 3) - 拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission;

-- =============================================
-- 更新现有用户的角色 (根据 userType)
-- userType: 1=普通用户, 2=管理员, 3=商家
-- =============================================
-- 给现有普通用户分配用户角色
INSERT INTO sys_user_role (principal_type, user_id, role_id)
SELECT 'platform', id, 1 FROM sys_user WHERE user_type = 1 AND deleted = 0;

-- 管理端管理员角色由后续 platform_user 初始化脚本授予。
-- Admin Sa-Token loginId 使用 admin:<platform_user.id>，不再使用 sys_user.id。

-- 如果有商家用户 (假设 userType=3 是商家)
-- INSERT INTO sys_user_role (principal_type, user_id, role_id)
-- SELECT 'merchant', id, 2 FROM sys_user WHERE user_type = 3 AND deleted = 0;

-- =============================================
-- v1 绠＄悊绔渶灏忛棴鐜ˉ鍏呮潈闄?
-- =============================================
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('admin:user:list', '平台用户列表', 'admin', '查看平台用户列表与详情'),
('admin:user:update', '平台用户状态维护', 'admin', '启用或禁用平台用户'),
('admin:trade:overview', '交易总览', 'admin', '查看平台交易聚合数据'),
('admin:trade:list', '交易列表', 'admin', '查看订单、支付单、充值单列表'),
('admin:trade:detail', '交易详情', 'admin', '查看订单详情'),
('admin:compensation:list', '补偿任务查询', 'admin', '查看补偿任务和重试任务列表'),
('admin:compensation:operate', '补偿任务操作', 'admin', '重试或取消补偿任务和重试任务');

-- 授予管理员角色对新增权限的访问权（幂等，重复执行不报错）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE permission_code IN
    ('admin:compensation:list', 'admin:compensation:operate');
