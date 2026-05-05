CREATE TABLE sys_user_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_permission (user_id, permission_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户额外权限关联表';

-- 插入管理权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
('admin:user:permission', '用户权限管理', '系统管理', '允许管理用户的额外权限'),
('admin:permission:list', '权限列表查询', '系统管理', '允许查询系统所有权限');

