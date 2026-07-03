package com.payment.rag.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 一次性工具：清理 Flyway 失败的迁移记录。
 * 连接信息必须从环境变量读取，避免把数据库凭据写入源码。
 */
public class FlywayRepair {

    public static void main(String[] args) throws Exception {
        String url = requiredEnv("FLYWAY_REPAIR_DB_URL");
        String user = requiredEnv("FLYWAY_REPAIR_DB_USERNAME");
        String password = requiredEnv("FLYWAY_REPAIR_DB_PASSWORD");

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("=== flyway_schema_history (failed) ===");
            var rs = stmt.executeQuery("SELECT * FROM flyway_schema_history WHERE success = 0");
            while (rs.next()) {
                System.out.printf("  version=%s, description=%s, success=%d%n",
                        rs.getString("version"), rs.getString("description"), rs.getInt("success"));
            }

            int deleted = stmt.executeUpdate("DELETE FROM flyway_schema_history WHERE success = 0");
            System.out.println("Deleted " + deleted + " failed migration record(s).");
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
