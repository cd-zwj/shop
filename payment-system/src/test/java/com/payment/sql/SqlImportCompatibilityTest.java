package com.payment.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlImportCompatibilityTest {

    @Test
    void numberedImportScriptsShouldNotUseMariaDbOnlyConditionalColumnSyntax() throws IOException {
        List<Path> sqlFiles;
        try (var paths = Files.list(Path.of("sql"))) {
            sqlFiles = paths
                    .filter(path -> path.getFileName().toString().matches("\\d{2}_.+\\.sql"))
                    .toList();
        }

        for (Path sqlFile : sqlFiles) {
            String sql = Files.readString(sqlFile).toUpperCase();
            assertFalse(sql.contains("ADD COLUMN IF NOT EXISTS"),
                    sqlFile.getFileName() + " contains MariaDB-only ADD COLUMN IF NOT EXISTS");
        }
    }

    @Test
    void flywayMigrationsShouldNotUseMariaDbOnlyConditionalColumnSyntax() throws IOException {
        Path migrationDirectory = Path.of("src", "main", "resources", "db", "migration");
        List<Path> migrationFiles;
        try (var paths = Files.list(migrationDirectory)) {
            migrationFiles = paths.filter(path -> path.toString().endsWith(".sql")).toList();
        }

        for (Path migrationFile : migrationFiles) {
            String sql = Files.readString(migrationFile).toUpperCase();
            assertFalse(sql.contains("DROP COLUMN IF EXISTS"),
                    migrationFile.getFileName() + " contains MariaDB-only DROP COLUMN IF EXISTS");
        }
    }

    @Test
    void repeatedMerchantBalanceVersionMigrationShouldBeGuarded() throws IOException {
        for (String fileName : List.of(
                "26_coupon_records_and_rules.sql",
                "29_merchant_balance_version.sql")) {
            assertColumnMigrationIsGuarded(fileName, "merchant_balance.version");
        }
    }

    @Test
    void repeatedOrderDeliveryProductNameMigrationShouldBeGuarded() throws IOException {
        assertColumnMigrationIsGuarded(
                "37_order_delivery_product_name.sql",
                "order_delivery_record.product_name");
    }

    @Test
    void legacyImportSnapshotShouldHandOffToFlywayAtVersion12() throws IOException {
        String applicationYaml = Files.readString(Path.of("src", "main", "resources", "application.yml"));
        String deliverySql = Files.readString(Path.of("sql", "32_product_delivery_framework.sql"));
        String walletSql = Files.readString(Path.of("sql", "14_platform_wallet_v1.sql"));
        String importAllSql = Files.readString(Path.of("sql", "import_all.sql"));
        String initSql = Files.readString(Path.of("sql", "00_init_database.sql"));

        assertTrue(applicationYaml.contains("baseline-version: 12"),
                "the manual SQL snapshot maps to Flyway version 12");
        assertTrue(applicationYaml.contains("baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:false}"),
                "automatic baselining must be an explicit bootstrap operation");
        assertFalse(importAllSql.contains("SOURCE 38_asset_activity_query_indexes.sql"),
                "V16 must exclusively own asset activity indexes");
        assertTrue(initSql.contains("CREATE TABLE IF NOT EXISTS `auth_user`"),
                "the baseline snapshot must retain the V1 auth_user table");
        assertFalse(deliverySql.contains("pickup_code_hash"),
                "V25 must exclusively own pickup-code hash columns and indexes");
        assertFalse(walletSql.contains("idx_sales_order_expire_scan"),
                "V26 must exclusively own the unpaid-order expiration scan index");
    }

    @Test
    void forwardMigrationShouldRemoveRestoredVirtualProductAndPosArtifacts() throws IOException {
        String cleanupSql = Files.readString(Path.of(
                "src", "main", "resources", "db", "migration",
                "V28__remove_legacy_virtual_product_and_pos_schema.sql"));

        assertTrue(cleanupSql.contains("virtual_product_type"));
        assertTrue(cleanupSql.contains("virtual_type_id"));
        assertTrue(cleanupSql.contains("cashier_id"));
        assertTrue(cleanupSql.contains("pos:checkout"));
    }

    @Test
    void productStockMigrationShouldBackfillAndVerifyBeforeDroppingLegacyTable() throws IOException {
        String migrationSql = Files.readString(Path.of(
                "src", "main", "resources", "db", "migration",
                "V21__remove_product_level_stock.sql"));

        assertTrue(migrationSql.contains("INSERT INTO store_product_stock"));
        assertTrue(migrationSql.contains("INSERT INTO store_product"));
        assertTrue(migrationSql.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(migrationSql.contains("DROP PROCEDURE IF EXISTS migrate_legacy_product_stock"));
        assertTrue(migrationSql.contains("JOIN store_product target_relation"));
        assertTrue(migrationSql.contains("legacy_stock.quantity < 0"));
        assertTrue(migrationSql.contains("legacy_stock.version < 0"));
        assertTrue(migrationSql.indexOf("INSERT INTO store_product_stock")
                        < migrationSql.indexOf("DROP TABLE product_stock"),
                "legacy stock may only be dropped after backfill");
    }

    private void assertColumnMigrationIsGuarded(String fileName, String columnName) throws IOException {
        String sql = Files.readString(Path.of("sql", fileName)).toUpperCase();

        assertTrue(sql.contains("INFORMATION_SCHEMA.COLUMNS"),
                fileName + " must check whether " + columnName + " already exists");
    }
}
