package com.payment.mysql;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = "MYSQL_MIGRATION_IT_ENABLED", matches = "true")
class MySqlFlywayMigrationIntegrationTest {

    private static final long TENANT_ID = 991001L;
    private static final long PRODUCT_ID = 992001L;
    private static final String GUARD_TABLE = "__mysql_migration_it_guard";

    @Test
    void manualSnapshotShouldUpgradeThroughConfiguredMigrationPath() {
        MigrationEnvironment environment = MigrationEnvironment.load();
        JdbcTemplate jdbcTemplate = environment.jdbcTemplate();
        assertThat(jdbcTemplate.queryForObject("SELECT DATABASE()", String.class))
                .isEqualTo("payment_db");
        assertDisposableGuard(jdbcTemplate, environment.guardToken());

        if ("historical".equals(environment.scenario())) {
            migrate(environment, "19");
            seedVersion19Inventory(jdbcTemplate);
            assertLegacyProductStockShape(jdbcTemplate);
        } else {
            assertThat(environment.scenario()).isEqualTo("fresh");
        }

        Flyway flyway = migrate(environment, null);
        flyway.validate();
        assertFinalSchema(jdbcTemplate, flyway);
        if ("historical".equals(environment.scenario())) {
            assertHistoricalInventoryMigrated(jdbcTemplate);
        }
        jdbcTemplate.execute("DROP TABLE " + GUARD_TABLE);
    }

    private Flyway migrate(MigrationEnvironment environment, String targetVersion) {
        var configuration = Flyway.configure()
                .dataSource(environment.url(), environment.username(), environment.password())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("12")
                .cleanDisabled(true);
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        Flyway flyway = configuration.load();
        flyway.migrate();
        return flyway;
    }

    private void seedVersion19Inventory(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                INSERT INTO tenant (id, tenant_code, name, status, deleted)
                VALUES (?, ?, ?, 1, 0)
                """, TENANT_ID, "MYSQL-CI-HISTORY", "MySQL CI history tenant");
        jdbcTemplate.update("""
                INSERT INTO product
                    (id, tenant_id, product_code, name, price, status, deleted)
                VALUES (?, ?, ?, ?, 25.80, 1, 0)
                """, PRODUCT_ID, TENANT_ID, "MYSQL-CI-PRODUCT", "MySQL CI product");
        jdbcTemplate.execute("""
                CREATE TABLE product_stock (
                    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
                    tenant_id BIGINT(20) NOT NULL COMMENT 'tenant id',
                    product_id BIGINT(20) NOT NULL COMMENT 'product id',
                    quantity INT NOT NULL DEFAULT '0' COMMENT 'stock quantity',
                    version INT NOT NULL DEFAULT '0' COMMENT 'optimistic lock version',
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_product_id (product_id),
                    KEY idx_tenant_id (tenant_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='legacy product stock'
                """);
        jdbcTemplate.update("""
                INSERT INTO product_stock (tenant_id, product_id, quantity, version)
                VALUES (?, ?, 37, 4)
                """, TENANT_ID, PRODUCT_ID);
    }

    private void assertDisposableGuard(JdbcTemplate jdbcTemplate, String expectedToken) {
        assertThat(tableExists(jdbcTemplate, GUARD_TABLE))
                .as("disposable migration database guard table")
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT guard_token FROM " + GUARD_TABLE, String.class))
                .isEqualTo(expectedToken);
    }

    private void assertLegacyProductStockShape(JdbcTemplate jdbcTemplate) {
        assertThat(columnExists(jdbcTemplate, "product_stock", "update_time")).isTrue();
        assertThat(indexColumns(jdbcTemplate, "product_stock", "uk_product_id"))
                .containsExactly("product_id");
        assertThat(indexColumns(jdbcTemplate, "product_stock", "idx_tenant_id"))
                .containsExactly("tenant_id");
    }

    private void assertFinalSchema(JdbcTemplate jdbcTemplate, Flyway flyway) {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("30");
        assertThat(tableExists(jdbcTemplate, "tenant_employee_store")).isTrue();
        assertThat(columnExists(jdbcTemplate, "tenant_employee", "store_scope_type")).isTrue();
        assertThat(columnExists(jdbcTemplate, "order_delivery_record", "pickup_code_hash")).isTrue();
        assertThat(columnExists(jdbcTemplate, "sales_order", "cashier_id")).isFalse();
        assertThat(columnExists(jdbcTemplate, "product", "virtual_product_type")).isFalse();
        assertThat(tableExists(jdbcTemplate, "product_stock")).isFalse();
    }

    private void assertHistoricalInventoryMigrated(JdbcTemplate jdbcTemplate) {
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM store_product_stock
                        WHERE tenant_id = ? AND product_id = ?
                          AND quantity = 37 AND version = 4
                        """, Long.class, TENANT_ID, PRODUCT_ID))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM store_product
                        WHERE tenant_id = ? AND product_id = ?
                        """, Long.class, TENANT_ID, PRODUCT_ID))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM store
                        WHERE tenant_id = ? AND store_no = ? AND deleted = 0
                        """, Long.class, TENANT_ID, "MIGRATED-" + TENANT_ID))
                .isEqualTo(1L);
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Long.class, tableName) == 1L;
    }

    private boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Long.class, tableName, columnName) == 1L;
    }

    private java.util.List<String> indexColumns(
            JdbcTemplate jdbcTemplate, String tableName, String indexName) {
        return jdbcTemplate.queryForList("""
                SELECT column_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                ORDER BY seq_in_index
                """, String.class, tableName, indexName);
    }

    private record MigrationEnvironment(
            String scenario, String url, String username, String password, String guardToken) {

        private static MigrationEnvironment load() {
            if (!Boolean.parseBoolean(required("MYSQL_MIGRATION_IT_DISPOSABLE"))) {
                throw new IllegalStateException("MySQL migration tests require a disposable database");
            }
            String scenario = required("MYSQL_MIGRATION_IT_SCENARIO").trim().toLowerCase();
            String url = required("MYSQL_MIGRATION_IT_URL");
            validateUrl(url);
            return new MigrationEnvironment(
                    scenario,
                    url,
                    required("MYSQL_MIGRATION_IT_USER"),
                    required("MYSQL_MIGRATION_IT_PASSWORD"),
                    required("MYSQL_MIGRATION_IT_GUARD_TOKEN"));
        }

        private JdbcTemplate jdbcTemplate() {
            return new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        }

        private static void validateUrl(String jdbcUrl) {
            try {
                URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
                if (!"mysql".equals(uri.getScheme())
                        || !("127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost()))
                        || uri.getPort() <= 0 || uri.getPort() == 3306
                        || !"/payment_db".equals(uri.getPath())) {
                    throw new IllegalStateException("invalid disposable MySQL migration URL");
                }
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                        "MYSQL_MIGRATION_IT_URL must target disposable localhost payment_db on a non-default port");
            }
        }

        private static String required(String name) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(name + " must be configured");
            }
            return value;
        }
    }
}
