-- Ensure built-in test platform accounts are linked by username instead of hard-coded ids.

SET @default_tenant_id = (
    SELECT id FROM tenant WHERE tenant_code = 'TENANT_001' LIMIT 1
);

INSERT INTO tenant_employee (tenant_id, platform_user_id, employee_no, employee_role, status)
SELECT @default_tenant_id, pu.id, 'EMP001', 'OWNER', 1
FROM platform_user pu
WHERE pu.username = 'merchant'
  AND pu.deleted = 0
  AND @default_tenant_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    employee_role = VALUES(employee_role),
    status = 1;

INSERT INTO tenant_member (tenant_id, platform_user_id, member_no, member_status, register_source)
SELECT @default_tenant_id, pu.id, 'MEM001', 1, 'APP'
FROM platform_user pu
WHERE pu.username = 'user'
  AND pu.deleted = 0
  AND @default_tenant_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    member_status = VALUES(member_status);

INSERT INTO tenant_member (tenant_id, platform_user_id, member_no, member_status, register_source)
SELECT @default_tenant_id, pu.id, 'MEM002', 1, 'APP'
FROM platform_user pu
WHERE pu.username = 'user2'
  AND pu.deleted = 0
  AND @default_tenant_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    member_status = VALUES(member_status);
