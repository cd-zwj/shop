-- Add account-system isolation to RBAC assignments.
-- Admin, merchant, and platform principals all use platform_user.id after this migration.

CREATE TABLE IF NOT EXISTS sys_user_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'User ID within principal_type namespace',
    permission_id BIGINT NOT NULL COMMENT 'Permission ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_permission (user_id, permission_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User extra permission relation table';

ALTER TABLE sys_user_role
    ADD COLUMN principal_type VARCHAR(32) NOT NULL DEFAULT 'platform' COMMENT 'Account system: admin, merchant, platform' AFTER id;

ALTER TABLE sys_user_permission
    ADD COLUMN principal_type VARCHAR(32) NOT NULL DEFAULT 'platform' COMMENT 'Account system: admin, merchant, platform' AFTER id;

-- Remove built-in account grants before re-inserting them with explicit principal_type.
-- This avoids the legacy (user_id, role_id) unique key merging admin/merchant/platform rows.
DELETE ur
FROM sys_user_role ur
JOIN sys_role r ON r.id = ur.role_id AND r.role_code IN ('admin', 'merchant', 'user')
JOIN platform_user pu ON pu.id = ur.user_id AND pu.username IN ('admin', 'merchant', 'user', 'user2') AND pu.deleted = 0;

-- Remove legacy admin grants that came from sys_user.id. Admin login now uses platform_user.id.
DELETE ur
FROM sys_user_role ur
JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'admin'
JOIN sys_user su ON su.id = ur.user_id AND su.user_type = 2 AND su.deleted = 0;

-- Built-in admin account: admin:<platform_user.id> must own the admin role.
INSERT INTO sys_user_role (principal_type, user_id, role_id)
SELECT 'admin', pu.id, r.id
FROM platform_user pu
JOIN sys_role r ON r.role_code = 'admin'
WHERE pu.username = 'admin'
  AND pu.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Built-in merchant account: merchant:<platform_user.id> owns the merchant role.
INSERT INTO sys_user_role (principal_type, user_id, role_id)
SELECT 'merchant', pu.id, r.id
FROM platform_user pu
JOIN sys_role r ON r.role_code = 'merchant'
WHERE pu.username = 'merchant'
  AND pu.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Built-in user accounts remain platform principals.
INSERT INTO sys_user_role (principal_type, user_id, role_id)
SELECT 'platform', pu.id, r.id
FROM platform_user pu
JOIN sys_role r ON r.role_code = 'user'
WHERE pu.username IN ('user', 'user2')
  AND pu.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Any remaining pre-existing merchant role grants belong to the merchant Sa-Token account.
UPDATE sys_user_role ur
JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'merchant'
SET ur.principal_type = 'merchant'
WHERE ur.principal_type = 'platform';

-- Existing user role grants remain platform grants.
UPDATE sys_user_role ur
JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'user'
SET ur.principal_type = 'platform';

-- Existing direct permissions are managed from the platform-user admin page.
UPDATE sys_user_permission
SET principal_type = 'platform'
WHERE principal_type IS NULL OR principal_type = '';

ALTER TABLE sys_user_role
    DROP INDEX uk_user_role,
    ADD UNIQUE KEY uk_principal_role (principal_type, user_id, role_id),
    ADD KEY idx_principal_user (principal_type, user_id);

ALTER TABLE sys_user_permission
    DROP INDEX uk_user_permission,
    ADD UNIQUE KEY uk_principal_permission (principal_type, user_id, permission_id),
    ADD KEY idx_principal_user (principal_type, user_id);
