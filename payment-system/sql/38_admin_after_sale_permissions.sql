-- ========================================
-- Platform after-sale operations permissions
-- ========================================

USE `payment_db`;

ALTER TABLE refund_application
  MODIFY COLUMN reject_reason VARCHAR(1000) DEFAULT NULL COMMENT '拒绝原因',
  ADD INDEX idx_refund_application_admin_time (create_time, id),
  ADD INDEX idx_refund_application_admin_status_time (refund_status, create_time, id);

INSERT INTO sys_permission (permission_code, permission_name, module, description)
VALUES
    ('admin:after-sale:list', '平台售后查询', 'admin', '跨租户查询售后申请与处理流水'),
    ('admin:after-sale:manage', '平台售后处理', 'admin', '平台人工介入售后退款或驳回')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    module = VALUES(module),
    description = VALUES(description);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM sys_role role_record
JOIN sys_permission permission_record
  ON permission_record.permission_code IN ('admin:after-sale:list', 'admin:after-sale:manage')
WHERE role_record.role_code = 'admin'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
