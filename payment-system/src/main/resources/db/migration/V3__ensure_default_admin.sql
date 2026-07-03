-- Ensure the local/default administrator account can log in.
-- Password: admin123. Change it immediately outside local/test environments.

INSERT INTO tenant (tenant_code, name, contact, status)
SELECT 'TENANT_001', '默认商户', '管理员', 1
WHERE NOT EXISTS (
    SELECT 1 FROM tenant WHERE tenant_code = 'TENANT_001'
);

SET @default_tenant_id = (
    SELECT id FROM tenant WHERE tenant_code = 'TENANT_001' LIMIT 1
);

UPDATE sys_user
SET password = '$2a$10$RM41BiXeQbfAcRJbjh2g2O6Z9lHJEO/Or6b40U8p.BXHmbSwfs5Ky',
    status = 1,
    deleted = 0
WHERE username = 'admin'
  AND user_type = 2;

INSERT INTO sys_user (tenant_id, username, password, nickname, user_type, status, deleted)
VALUES (
    @default_tenant_id,
    'admin',
    '$2a$10$RM41BiXeQbfAcRJbjh2g2O6Z9lHJEO/Or6b40U8p.BXHmbSwfs5Ky',
    '管理员',
    2,
    1,
    0
)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    nickname = COALESCE(nickname, VALUES(nickname)),
    user_type = 2,
    status = 1,
    deleted = 0;

INSERT INTO sys_role (role_code, role_name, description)
SELECT 'admin', '管理员', '平台管理员，可以管理商家、审核提现等'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'admin'
);

SET @admin_role_id = (
    SELECT id FROM sys_role WHERE role_code = 'admin' LIMIT 1
);

INSERT INTO sys_user_role (user_id, role_id)
SELECT id, @admin_role_id
FROM sys_user
WHERE user_type = 2
  AND deleted = 0
  AND @admin_role_id IS NOT NULL
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
