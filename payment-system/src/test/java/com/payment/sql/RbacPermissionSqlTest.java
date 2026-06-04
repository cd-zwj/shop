package com.payment.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RbacPermissionSqlTest {

    @Test
    void rbacSeedShouldContainAdminMarketingPermissionsAndRoleGrant() throws IOException {
        String sql = Files.readString(Path.of("sql", "12_rbac_permission.sql"));

        assertTrue(sql.contains("'admin:marketing:list'"));
        assertTrue(sql.contains("'admin:marketing:create'"));
        assertTrue(sql.contains("'admin:marketing:update'"));
        assertTrue(sql.contains("SELECT 3, id FROM sys_permission WHERE permission_code IN"));
        assertTrue(sql.contains("'admin:marketing:list', 'admin:marketing:create', 'admin:marketing:update'"));
    }
}
