package com.payment.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlywayMigrationChecksumTest {

    private static final Path MIGRATION_DIRECTORY =
            Path.of("src", "main", "resources", "db", "migration");

    private static final Map<String, Integer> APPLIED_MIGRATIONS = Map.ofEntries(
            Map.entry("V1__baseline.sql", 1622125474),
            Map.entry("V2__add_auth_user_email.sql", -2073502175),
            Map.entry("V2_1__ensure_identity_rbac_core_tables.sql", -760660126),
            Map.entry("V3__ensure_default_admin.sql", -2051772551),
            Map.entry("V4__ensure_platform_user_rbac.sql", -962143874),
            Map.entry("V5__fix_default_platform_account_passwords.sql", 1879648267),
            Map.entry("V6__ensure_default_platform_memberships.sql", 1477649622),
            Map.entry("V7__add_rbac_principal_type.sql", -1947199622),
            Map.entry("V8__virtual_product_taxonomy.sql", 520446376),
            Map.entry("V9__order_shipping_snapshot.sql", 2028820520),
            Map.entry("V10__order_delivery_product_name.sql", -841494383),
            Map.entry("V11__product_change_log.sql", -1201931703),
            Map.entry("V12__harden_rbac_principal_type_idempotency.sql", -670559849),
            Map.entry("V13__ensure_product_virtual_columns_and_rag_tables.sql", 1632789554),
            Map.entry("V14__coupon_member_rules_and_marketing_effect.sql", 415670884),
            Map.entry("V15__merchant_platform_fee.sql", 1212560683),
            Map.entry("V16__asset_activity_query_indexes.sql", 104456772),
            Map.entry("V17__add_sales_order_fulfillment_mode.sql", 900779155),
            Map.entry("V18__add_store_product_stock.sql", 1253749835),
            Map.entry("V19__add_pos_order_fields.sql", -1814218642),
            Map.entry("V20__add_store_product_relation.sql", 692266751),
            Map.entry("V21__remove_product_level_stock.sql", -1598131078),
            Map.entry("V22__add_order_fulfillment_action.sql", 56251928),
            Map.entry("V23__add_store_review.sql", 1050387349),
            Map.entry("V24__add_after_sale_evidence_and_action.sql", 600673378),
            Map.entry("V25__pickup_code_hash.sql", -1470136308),
            Map.entry("V26__order_expire_scan_index.sql", -677144994),
            Map.entry("V27__payment_callback_failure_audit.sql", -1601699441),
            Map.entry("V28__remove_legacy_virtual_product_and_pos_schema.sql", 595058280));

    @Test
    void historicallyAppliedMigrationsShouldRemainByteCompatible() throws IOException {
        for (Map.Entry<String, Integer> migration : APPLIED_MIGRATIONS.entrySet()) {
            Path migrationFile = MIGRATION_DIRECTORY.resolve(migration.getKey());
            assertEquals(migration.getValue(), checksum(migrationFile), migration.getKey());
        }
    }

    private int checksum(Path migrationFile) throws IOException {
        CRC32 crc32 = new CRC32();
        for (String line : Files.readAllLines(migrationFile)) {
            crc32.update(line.getBytes(StandardCharsets.UTF_8));
        }
        return (int) crc32.getValue();
    }
}
