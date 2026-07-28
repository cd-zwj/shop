ALTER TABLE tenant_employee
    ADD UNIQUE KEY uk_tenant_employee_identity (tenant_id, id);

ALTER TABLE store
    ADD UNIQUE KEY uk_store_tenant_identity (tenant_id, id);

ALTER TABLE tenant_employee_store
    ADD CONSTRAINT fk_employee_store_employee
        FOREIGN KEY (tenant_id, employee_id)
        REFERENCES tenant_employee (tenant_id, id),
    ADD CONSTRAINT fk_employee_store_store
        FOREIGN KEY (tenant_id, store_id)
        REFERENCES store (tenant_id, id);
