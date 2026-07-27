-- ========================================
-- 数据库初始化脚本
-- ========================================
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `payment_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `payment_db`;

-- 设置时区
SET time_zone = '+08:00';

-- 设置字符集
SET NAMES utf8mb4;

-- Flyway V1-V2 baseline object. The manual snapshot is handed off at V12.
CREATE TABLE IF NOT EXISTS `auth_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
  `password` VARCHAR(255) NOT NULL,
  `status` INT DEFAULT '1',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
