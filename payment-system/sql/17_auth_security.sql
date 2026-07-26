-- ========================================
-- 登录与安全模块
-- 基于 v1 平台用户模型扩展
-- ========================================

USE `payment_db`;

-- 短信验证码表
CREATE TABLE IF NOT EXISTS `sms_verify_code` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务编号',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `biz_type` VARCHAR(20) NOT NULL COMMENT '业务类型：LOGIN-登录，REGISTER-注册，RESET_PASSWORD-找回密码，BIND_PHONE-绑定手机号',
  `verify_code` VARCHAR(16) NOT NULL COMMENT '验证码',
  `send_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态：PENDING-待发送，SUCCESS-成功，FAIL-失败',
  `verify_status` VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT '校验状态：UNUSED-未使用，USED-已使用，EXPIRED-已过期',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
  `verify_time` DATETIME DEFAULT NULL COMMENT '验证时间',
  `request_ip` VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sms_biz_no` (`biz_no`),
  KEY `idx_sms_phone_biz_type` (`phone`, `biz_type`, `create_time`),
  KEY `idx_sms_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码表';

-- 登录会话表
CREATE TABLE IF NOT EXISTS `login_session` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_no` VARCHAR(64) NOT NULL COMMENT '会话编号',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `terminal_type` VARCHAR(20) NOT NULL COMMENT '终端类型：APP、H5、MINI_PROGRAM、PC',
  `access_token` VARCHAR(255) NOT NULL COMMENT '访问令牌',
  `refresh_token` VARCHAR(255) DEFAULT NULL COMMENT '刷新令牌',
  `session_status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态：ACTIVE-有效，EXPIRED-过期，LOGOUT-已退出，REVOKED-已吊销',
  `login_ip` VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
  `login_region` VARCHAR(128) DEFAULT NULL COMMENT '登录地区',
  `device_id` VARCHAR(128) DEFAULT NULL COMMENT '设备ID',
  `device_name` VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  `last_active_time` DATETIME DEFAULT NULL COMMENT '最后活跃时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `logout_time` DATETIME DEFAULT NULL COMMENT '退出时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_login_session_no` (`session_no`),
  UNIQUE KEY `uk_login_access_token` (`access_token`),
  UNIQUE KEY `uk_login_refresh_token` (`refresh_token`),
  KEY `idx_login_session_user` (`platform_user_id`, `session_status`),
  KEY `idx_login_session_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录会话表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS `user_login_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_user_id` BIGINT(20) DEFAULT NULL COMMENT '平台用户ID',
  `login_type` VARCHAR(20) NOT NULL COMMENT '登录方式：PASSWORD-密码，SMS-短信，WECHAT-微信，GITHUB-GitHub，APPLE-Apple',
  `login_account` VARCHAR(128) DEFAULT NULL COMMENT '登录账号标识',
  `login_status` VARCHAR(20) NOT NULL COMMENT '登录状态：SUCCESS-成功，FAIL-失败',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `login_ip` VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
  `login_region` VARCHAR(128) DEFAULT NULL COMMENT '登录地区',
  `device_id` VARCHAR(128) DEFAULT NULL COMMENT '设备ID',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  `is_unusual` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否异常登录',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_login_log_user` (`platform_user_id`, `create_time`),
  KEY `idx_user_login_log_account` (`login_account`, `create_time`),
  KEY `idx_user_login_log_ip` (`login_ip`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表';

-- 登录失败控制表
CREATE TABLE IF NOT EXISTS `login_fail_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_type` VARCHAR(20) NOT NULL COMMENT '账号类型：USERNAME-用户名，PHONE-手机号，AUTH_KEY-第三方认证Key',
  `account_value` VARCHAR(128) NOT NULL COMMENT '账号值',
  `fail_count` INT NOT NULL DEFAULT '0' COMMENT '连续失败次数',
  `last_fail_time` DATETIME DEFAULT NULL COMMENT '最后失败时间',
  `lock_start_time` DATETIME DEFAULT NULL COMMENT '锁定开始时间',
  `lock_end_time` DATETIME DEFAULT NULL COMMENT '锁定结束时间',
  `lock_status` VARCHAR(20) NOT NULL DEFAULT 'UNLOCKED' COMMENT '锁定状态：UNLOCKED-未锁定，LOCKED-已锁定',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_login_fail_account` (`account_type`, `account_value`),
  KEY `idx_login_fail_lock_status` (`lock_status`, `lock_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录失败控制表';

-- 密码重置记录表
CREATE TABLE IF NOT EXISTS `password_reset_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reset_no` VARCHAR(64) NOT NULL COMMENT '重置编号',
  `platform_user_id` BIGINT(20) NOT NULL COMMENT '平台用户ID',
  `verify_biz_no` VARCHAR(64) DEFAULT NULL COMMENT '验证码业务编号',
  `reset_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '重置状态：PENDING-待重置，SUCCESS-成功，FAIL-失败，CANCELLED-取消',
  `request_ip` VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `reset_time` DATETIME DEFAULT NULL COMMENT '重置时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_password_reset_no` (`reset_no`),
  KEY `idx_password_reset_user` (`platform_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置记录表';
