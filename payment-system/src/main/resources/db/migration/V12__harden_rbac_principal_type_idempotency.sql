-- Harden RBAC principal_type migration for databases that were partially migrated before.
-- Flyway runs each version once, but local/stale schemas may already contain some of
-- these columns or indexes. Guard every DDL statement through information_schema.

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_user_role ADD COLUMN principal_type VARCHAR(32) NOT NULL DEFAULT ''platform'' COMMENT ''Account system: admin, merchant, platform'' AFTER id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'sys_user_role'
      AND column_name = 'principal_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_user_permission ADD COLUMN principal_type VARCHAR(32) NOT NULL DEFAULT ''platform'' COMMENT ''Account system: admin, merchant, platform'' AFTER id',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'sys_user_permission'
      AND column_name = 'principal_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_user_role
SET principal_type = 'platform'
WHERE principal_type IS NULL OR principal_type = '';

UPDATE sys_user_permission
SET principal_type = 'platform'
WHERE principal_type IS NULL OR principal_type = '';

SET @sql = (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE sys_user_role DROP INDEX uk_user_role',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'sys_user_role'
      AND index_name = 'uk_user_role'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE sys_user_permission DROP INDEX uk_user_permission',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'sys_user_permission'
      AND index_name = 'uk_user_permission'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_user_role ADD UNIQUE KEY uk_principal_role (principal_type, user_id, role_id)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'sys_user_role'
      AND index_name = 'uk_principal_role'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_user_role ADD KEY idx_principal_user (principal_type, user_id)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'sys_user_role'
      AND index_name = 'idx_principal_user'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_user_permission ADD UNIQUE KEY uk_principal_permission (principal_type, user_id, permission_id)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'sys_user_permission'
      AND index_name = 'uk_principal_permission'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_user_permission ADD KEY idx_principal_user (principal_type, user_id)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'sys_user_permission'
      AND index_name = 'idx_principal_user'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
