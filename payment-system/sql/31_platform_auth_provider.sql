USE `payment_db`;

CREATE TABLE IF NOT EXISTS `platform_auth_provider` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider_code` VARCHAR(32) NOT NULL COMMENT '登录方式编码',
  `provider_name` VARCHAR(64) NOT NULL COMMENT '登录方式名称',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
  `sort_order` INT NOT NULL DEFAULT '0' COMMENT '排序',
  `app_id` VARCHAR(128) DEFAULT NULL COMMENT '第三方应用ID',
  `client_id` VARCHAR(128) DEFAULT NULL COMMENT '第三方客户端ID',
  `redirect_uri` VARCHAR(255) DEFAULT NULL COMMENT '回调地址',
  `ext_json` TEXT COMMENT '扩展配置',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_code` (`provider_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台第三方登录方式表';

SET @schema_name = DATABASE();
SET @provider_id_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'platform_user_auth'
      AND column_name = 'provider_id'
);
SET @provider_id_sql = IF(
    @provider_id_exists = 0,
    'ALTER TABLE `platform_user_auth` ADD COLUMN `provider_id` BIGINT(20) DEFAULT NULL COMMENT ''第三方登录方式ID'' AFTER `platform_user_id`',
    'SELECT 1'
);
PREPARE provider_column_stmt FROM @provider_id_sql;
EXECUTE provider_column_stmt;
DEALLOCATE PREPARE provider_column_stmt;

SET @provider_key_index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'platform_user_auth'
      AND index_name = 'uk_provider_auth_key'
);
SET @provider_key_index_sql = IF(
    @provider_key_index_exists = 0,
    'ALTER TABLE `platform_user_auth` ADD UNIQUE KEY `uk_provider_auth_key` (`provider_id`, `auth_key`)',
    'SELECT 1'
);
PREPARE provider_stmt FROM @provider_key_index_sql;
EXECUTE provider_stmt;
DEALLOCATE PREPARE provider_stmt;

SET @provider_union_index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'platform_user_auth'
      AND index_name = 'uk_provider_union_key'
);
SET @provider_union_index_sql = IF(
    @provider_union_index_exists = 0,
    'ALTER TABLE `platform_user_auth` ADD UNIQUE KEY `uk_provider_union_key` (`provider_id`, `auth_union_key`)',
    'SELECT 1'
);
PREPARE provider_union_stmt FROM @provider_union_index_sql;
EXECUTE provider_union_stmt;
DEALLOCATE PREPARE provider_union_stmt;
