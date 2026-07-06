-- ========================================
-- 完整数据库导入脚本
-- 按顺序执行所有模块的 SQL 文件
-- ========================================

-- 使用方法：
-- mysql -u root -p < payment-system/sql/import_all.sql

SOURCE 00_init_database.sql;
SOURCE 01_tenant_user.sql;
SOURCE 02_product.sql;
SOURCE 03_order.sql;
SOURCE 04_user_balance.sql;
SOURCE 05_user_points.sql;
SOURCE 06_recharge.sql;
SOURCE 07_exchange.sql;
SOURCE 08_merchant_finance.sql;
SOURCE 09_pos.sql;
SOURCE 10_analytics.sql;
SOURCE 11_message_idempotent.sql;
SOURCE 12_rbac_permission.sql;
SOURCE 13_user_permission.sql;
SOURCE 14_platform_wallet_v1.sql;
SOURCE 15_coupon_marketing.sql;
SOURCE 16_refund.sql;
SOURCE 17_auth_security.sql;
SOURCE 18_store_membership.sql;
SOURCE 19_email_auth.sql;
SOURCE 20_message_retry.sql;
SOURCE 21_store_rating_migration.sql;
SOURCE 22_user_shipping_address.sql;
SOURCE 23_user_notification.sql;
SOURCE 24_refund_application.sql;
SOURCE 25_payment_bill_status_remark.sql;
SOURCE 26_coupon_records_and_rules.sql;
SOURCE 27_new_modules.sql;
SOURCE 28_permission_rate_limit_hardening.sql;
SOURCE 29_merchant_balance_version.sql;
SOURCE 30_critical_indexes.sql;
SOURCE 31_platform_auth_provider.sql;
SOURCE 32_product_delivery_framework.sql;
SOURCE 33_card_key_pool.sql;
SOURCE 34_ai_tool_permissions.sql;
SOURCE 35_virtual_product_taxonomy.sql;
SOURCE 36_order_shipping_snapshot.sql;
SOURCE 37_order_delivery_product_name.sql;
SOURCE 99_init_data.sql;

SELECT 'Database initialization completed successfully!' AS message;
