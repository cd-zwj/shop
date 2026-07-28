ALTER TABLE tenant_employee
    ADD COLUMN store_scope_type VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT '门店范围：ALL全部，ASSIGNED已分配' AFTER employee_role,
    ADD CONSTRAINT chk_tenant_employee_store_scope
        CHECK (store_scope_type IN ('ALL', 'ASSIGNED'));

CREATE TABLE tenant_employee_store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    employee_id BIGINT NOT NULL COMMENT '租户员工ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    created_by BIGINT NULL COMMENT '分配操作人平台用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_employee_store_scope (tenant_id, employee_id, store_id),
    KEY idx_employee_store_employee (tenant_id, employee_id),
    KEY idx_employee_store_store (tenant_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户员工门店授权';
