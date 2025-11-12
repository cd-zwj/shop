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
