package com.payment.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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

    @Test
    void rbacSqlShouldIncludePrincipalTypeColumnsAndIndexes() throws IOException {
        String roleSql = Files.readString(Path.of("sql", "12_rbac_permission.sql"));
        String permissionSql = Files.readString(Path.of("sql", "13_user_permission.sql"));
        String migrationSql = Files.readString(Path.of("src", "main", "resources", "db", "migration", "V7__add_rbac_principal_type.sql"));

        assertTrue(roleSql.contains("principal_type VARCHAR(32) NOT NULL DEFAULT 'platform'"));
        assertTrue(roleSql.contains("UNIQUE KEY uk_user_role (principal_type, user_id, role_id)"));
        assertTrue(permissionSql.contains("principal_type VARCHAR(32) NOT NULL DEFAULT 'platform'"));
        assertTrue(permissionSql.contains("UNIQUE KEY uk_user_permission (principal_type, user_id, permission_id)"));
        assertTrue(migrationSql.contains("ADD COLUMN principal_type VARCHAR(32) NOT NULL DEFAULT 'platform'"));
        assertTrue(migrationSql.contains("ADD UNIQUE KEY uk_principal_role (principal_type, user_id, role_id)"));
        assertTrue(migrationSql.contains("ADD UNIQUE KEY uk_principal_permission (principal_type, user_id, permission_id)"));
    }

    @Test
    void rbacPrincipalTypeHardeningMigrationShouldGuardColumnsAndIndexes() throws IOException {
        String migrationSql = Files.readString(Path.of(
                "src", "main", "resources", "db", "migration", "V12__harden_rbac_principal_type_idempotency.sql"));

        assertTrue(migrationSql.contains("information_schema.columns"));
        assertTrue(migrationSql.contains("information_schema.statistics"));
        assertTrue(migrationSql.contains("PREPARE stmt FROM @sql"));
        assertTrue(migrationSql.contains("column_name = 'principal_type'"));
        assertTrue(migrationSql.contains("index_name = 'uk_user_role'"));
        assertTrue(migrationSql.contains("index_name = 'uk_principal_role'"));
        assertTrue(migrationSql.contains("index_name = 'uk_principal_permission'"));
    }

    @Test
    void importAllShouldReferenceEveryNumberedSqlScript() throws IOException {
        String importAll = Files.readString(Path.of("sql", "import_all.sql"));
        try (Stream<Path> paths = Files.list(Path.of("sql"))) {
            paths
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.matches("\\d{2}_.+\\.sql"))
                    .filter(name -> !name.endsWith("_test_accounts.sql"))
                    .forEach(name -> assertTrue(importAll.contains("SOURCE " + name + ";"), name + " should be sourced"));
        }
    }
}
