-- Ensure core identity/RBAC tables exist before default-account data migrations.
-- V3-V7 insert into these tables directly, so a fresh Flyway deployment needs them before V3.

CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(50) NOT NULL COMMENT 'Tenant code',
    name VARCHAR(100) NOT NULL COMMENT 'Tenant name',
    contact VARCHAR(100) COMMENT 'Contact person',
    phone VARCHAR(20) COMMENT 'Contact phone',
    status INT DEFAULT 1 COMMENT 'Status: 0 disabled, 1 enabled',
    deleted INT DEFAULT 0 COMMENT 'Deleted flag',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_code (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant table';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT COMMENT 'Tenant ID',
    username VARCHAR(50) NOT NULL COMMENT 'Username',
    password VARCHAR(255) NOT NULL COMMENT 'Password hash',
    nickname VARCHAR(50) COMMENT 'Nickname',
    phone VARCHAR(20) COMMENT 'Phone',
    email VARCHAR(100) COMMENT 'Email',
    avatar VARCHAR(255) COMMENT 'Avatar URL',
    user_type INT NOT NULL DEFAULT 1 COMMENT '1 user, 2 admin, 3 merchant',
    status INT DEFAULT 1 COMMENT 'Status: 0 disabled, 1 enabled',
    deleted INT DEFAULT 0 COMMENT 'Deleted flag',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Legacy system user table';

CREATE TABLE IF NOT EXISTS platform_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_no VARCHAR(32) NOT NULL COMMENT 'Platform user number',
    username VARCHAR(64) NOT NULL COMMENT 'Username',
    phone VARCHAR(20) DEFAULT NULL COMMENT 'Phone',
    email VARCHAR(100) DEFAULT NULL COMMENT 'Email',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Password hash',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Deleted flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_no (user_no),
    UNIQUE KEY uk_platform_username (username),
    UNIQUE KEY uk_platform_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Platform user table';

CREATE TABLE IF NOT EXISTS tenant_employee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT 'Tenant ID',
    platform_user_id BIGINT NOT NULL COMMENT 'Platform user ID',
    employee_no VARCHAR(32) NOT NULL COMMENT 'Employee number',
    employee_role VARCHAR(32) NOT NULL COMMENT 'Employee role',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_platform_user (tenant_id, platform_user_id),
    UNIQUE KEY uk_employee_no (employee_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant employee relation table';

CREATE TABLE IF NOT EXISTS tenant_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT 'Tenant ID',
    platform_user_id BIGINT NOT NULL COMMENT 'Platform user ID',
    member_no VARCHAR(32) NOT NULL COMMENT 'Member number',
    member_status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
    member_level INT DEFAULT 1 COMMENT 'Member level',
    register_source VARCHAR(32) DEFAULT NULL COMMENT 'Register source',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_member (tenant_id, platform_user_id),
    UNIQUE KEY uk_member_no (member_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant member relation table';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL COMMENT 'Role code',
    role_name VARCHAR(100) NOT NULL COMMENT 'Role name',
    description VARCHAR(255) COMMENT 'Description',
    status INT DEFAULT 1 COMMENT 'Status: 0 disabled, 1 enabled',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role table';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL COMMENT 'Permission code',
    permission_name VARCHAR(100) NOT NULL COMMENT 'Permission name',
    module VARCHAR(50) COMMENT 'Module',
    description VARCHAR(255) COMMENT 'Description',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Permission table';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'User ID',
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User role relation table';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    permission_id BIGINT NOT NULL COMMENT 'Permission ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role permission relation table';
