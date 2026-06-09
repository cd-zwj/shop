-- ========================================
-- 新增模块：审计日志 / 租户配置 / 商品分类 / 文件管理
-- 以及 product / sales_order 表新增 store_id 字段
-- ========================================

USE `payment_db`;

-- ============================================================
-- 1. 运营审计日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_audit_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '租户ID（平台级操作可为NULL）',
  `operator_id` BIGINT(20) DEFAULT NULL COMMENT '操作人ID',
  `operator_type` VARCHAR(20) NOT NULL COMMENT '操作人类型: ADMIN/MERCHANT/USER/SYSTEM',
  `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人名称',
  `module` VARCHAR(50) NOT NULL COMMENT '模块名: MERCHANT/ORDER/PAYMENT等',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/APPROVE/REJECT',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型: Tenant/Order/Withdrawal等',
  `target_id` BIGINT(20) DEFAULT NULL COMMENT '目标ID',
  `detail` TEXT COMMENT '操作详情JSON',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_tenant_id` (`tenant_id`),
  KEY `idx_audit_operator_id` (`operator_id`),
  KEY `idx_audit_module_action` (`module`, `action`),
  KEY `idx_audit_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营审计日志表';

-- ============================================================
-- 2. 租户配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tenant_config` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) NOT NULL COMMENT '租户ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键: PAYMENT_CHANNEL/FEATURE_TOGGLE/BRAND_NAME等',
  `config_value` TEXT COMMENT '配置值（JSON格式）',
  `config_type` VARCHAR(20) NOT NULL DEFAULT 'CUSTOM' COMMENT '配置类型: SYSTEM/CUSTOM',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_config_key` (`tenant_id`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户配置表';

-- ============================================================
-- 3. 商品分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS `product_category` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '租户ID（NULL表示平台级分类）',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT(20) NOT NULL DEFAULT '0' COMMENT '父分类ID，0表示顶级',
  `sort_order` INT NOT NULL DEFAULT '0' COMMENT '排序值',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-否, 1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_tenant_id` (`tenant_id`),
  KEY `idx_category_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ============================================================
-- 4. 文件/媒体管理表
-- ============================================================
CREATE TABLE IF NOT EXISTS `file_asset` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT(20) DEFAULT NULL COMMENT '租户ID',
  `platform_user_id` BIGINT(20) DEFAULT NULL COMMENT '上传者用户ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_path` VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径',
  `file_md5` VARCHAR(64) DEFAULT NULL COMMENT '文件MD5',
  `file_size` BIGINT(20) DEFAULT NULL COMMENT '文件大小（字节）',
  `content_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
  `storage_type` VARCHAR(20) NOT NULL DEFAULT 'MINIO' COMMENT '存储类型: MINIO/OSS/LOCAL',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-已删除, 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_tenant_id` (`tenant_id`),
  KEY `idx_file_platform_user_id` (`platform_user_id`),
  KEY `idx_file_md5` (`file_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件媒体管理表';

-- ============================================================
-- 5. 已有表新增 store_id 字段
-- ============================================================

-- product 表新增 store_id
ALTER TABLE `product`
  ADD COLUMN `store_id` BIGINT(20) DEFAULT NULL COMMENT '门店ID' AFTER `description`,
  ADD KEY `idx_product_store_id` (`store_id`);

-- sales_order 表新增 store_id
ALTER TABLE `sales_order`
  ADD COLUMN `store_id` BIGINT(20) DEFAULT NULL COMMENT '门店ID' AFTER `expire_time`,
  ADD KEY `idx_sales_order_store_id` (`store_id`);
