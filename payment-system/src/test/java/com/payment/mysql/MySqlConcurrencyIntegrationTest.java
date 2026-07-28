package com.payment.mysql;

import com.payment.common.BusinessException;
import com.payment.config.MyBatisPlusConfig;
import com.payment.dto.RefundCreateDTO;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.RefundApplicationService;
import com.payment.service.RefundService;
import com.payment.service.MessageIdempotentService;
import com.payment.service.MessageClaimResult;
import com.payment.service.MessageClaim;
import com.payment.service.StoreInventoryService;
import com.payment.service.UserNotificationService;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.service.impl.MerchantStoreScopeService;
import com.payment.service.impl.MessageIdempotentServiceImpl;
import com.payment.service.impl.RefundApplicationServiceImpl;
import com.payment.service.impl.StoreInventoryServiceImpl;
import com.payment.util.TenantContextHolder;
import org.mybatis.spring.annotation.MapperScan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Tag("mysql-integration")
@SpringBootTest(classes = MySqlConcurrencyIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "MYSQL_IT_ENABLED", matches = "true")
class MySqlConcurrencyIntegrationTest {

    private static final long TENANT_ID = 910001L;
    private static final long USER_ID = 920001L;
    private static final long STORE_ID = 930001L;
    private static final long PRODUCT_ID = 940001L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SalesOrderMapper salesOrderMapper;

    @Autowired
    private StoreInventoryService storeInventoryService;

    @Autowired
    private RefundApplicationService refundApplicationService;

    @Autowired
    private MessageIdempotentService messageIdempotentService;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        String url = environment("MYSQL_IT_URL",
                "jdbc:mysql://127.0.0.1:3306/payment_mysql_it"
                        + "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8"
                        + "&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        URI mysqlUri = validateIsolatedDatabaseUrl(url);
        String username = environment("MYSQL_IT_USER", "root");
        String password = requiredEnvironment("MYSQL_IT_PASSWORD");
        recreateIsolatedDatabase(mysqlUri, username, password);
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
        registry.add("spring.flyway.enabled", () -> false);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:mysql-concurrency-schema.sql");
    }

    @BeforeEach
    void resetFixtures() {
        assertThat(jdbcTemplate.queryForObject("SELECT DATABASE()", String.class))
                .isEqualTo("payment_mysql_it");
        jdbcTemplate.update("DELETE FROM after_sale_action WHERE tenant_id = ?", TENANT_ID);
        jdbcTemplate.update("DELETE FROM refund_application WHERE tenant_id = ?", TENANT_ID);
        jdbcTemplate.update("DELETE FROM store_inventory_change_log WHERE tenant_id = ?", TENANT_ID);
        jdbcTemplate.update("DELETE FROM store_product_stock WHERE tenant_id = ?", TENANT_ID);
        jdbcTemplate.update("DELETE FROM sales_order_item WHERE tenant_id = ?", TENANT_ID);
        jdbcTemplate.update("DELETE FROM sales_order WHERE tenant_id = ?", TENANT_ID);
        jdbcTemplate.update("DELETE FROM message_idempotent");
    }

    @Test
    void competingPaymentSuccessFailureCancelAndTimeoutShouldHaveExactlyOneWinner() throws Exception {
        long orderId = insertOrder("SO-MYSQL-RACE", "CREATED", "WAIT_PAY");
        jdbcTemplate.update(
                "UPDATE sales_order SET expire_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE id = ?", orderId);

        List<Integer> changed = runConcurrently(List.of(
                () -> salesOrderMapper.claimPayment(orderId),
                () -> salesOrderMapper.failPayment(orderId),
                () -> salesOrderMapper.cancelUnpaid(orderId),
                () -> jdbcTemplate.update("""
                        UPDATE sales_order
                        SET order_status = 'CLOSED', pay_status = 'CLOSED', update_time = NOW()
                        WHERE id = ? AND tenant_id = ? AND deleted = 0
                          AND order_status = 'CREATED' AND pay_status = 'WAIT_PAY'
                          AND expire_time <= NOW()
                        """, orderId, TENANT_ID)));

        assertThat(changed).containsExactlyInAnyOrder(1, 0, 0, 0);
        var state = jdbcTemplate.queryForMap(
                "SELECT order_status, pay_status FROM sales_order WHERE id = ?", orderId);
        assertThat(List.of(
                state.get("order_status") + "/" + state.get("pay_status")))
                .allMatch(value -> List.of(
                                "PAID/SUCCESS", "CLOSED/FAILED", "CANCELLED/CLOSED", "CLOSED/CLOSED")
                        .contains(value));
    }

    @Test
    void concurrentDuplicateInventoryLockShouldBeIdempotent() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO store_product_stock
                    (tenant_id, store_id, product_id, quantity, locked_quantity, version)
                VALUES (?, ?, ?, 10, 0, 0)
                """, TENANT_ID, STORE_ID, PRODUCT_ID);

        List<Throwable> failures = runConcurrentlyCapturingFailures(2,
                () -> storeInventoryService.lock(
                        TENANT_ID, STORE_ID, PRODUCT_ID, 4, "SALES_ORDER", "SO-MYSQL-LOCK"));

        assertThat(failures).isEmpty();
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT locked_quantity FROM store_product_stock
                        WHERE tenant_id = ? AND store_id = ? AND product_id = ?
                        """, Integer.class, TENANT_ID, STORE_ID, PRODUCT_ID))
                .isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM store_inventory_change_log
                        WHERE tenant_id = ? AND store_id = ? AND product_id = ?
                          AND change_type = 'LOCK' AND biz_type = 'SALES_ORDER' AND biz_no = 'SO-MYSQL-LOCK'
                        """, Integer.class, TENANT_ID, STORE_ID, PRODUCT_ID))
                .isEqualTo(1);
    }

    @Test
    void concurrentMissingInventoryInitializationShouldCreateOneRow() throws Exception {
        List<Throwable> failures = runConcurrentlyCapturingFailures(2,
                () -> storeInventoryService.getOrCreate(TENANT_ID, STORE_ID, PRODUCT_ID));

        assertThat(failures).isEmpty();
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM store_product_stock
                        WHERE tenant_id = ? AND store_id = ? AND product_id = ?
                        """, Integer.class, TENANT_ID, STORE_ID, PRODUCT_ID))
                .isEqualTo(1);
    }

    @Test
    void concurrentDuplicateInventoryReleaseShouldBeIdempotent() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO store_product_stock
                    (tenant_id, store_id, product_id, quantity, locked_quantity, version)
                VALUES (?, ?, ?, 10, 0, 0)
                """, TENANT_ID, STORE_ID, PRODUCT_ID);
        TenantContextHolder.setTenantId(TENANT_ID);
        try {
            storeInventoryService.lock(
                    TENANT_ID, STORE_ID, PRODUCT_ID, 4, "SALES_ORDER", "SO-MYSQL-RELEASE");
        } finally {
            TenantContextHolder.clear();
        }

        List<Throwable> failures = runConcurrentlyCapturingFailures(2,
                () -> storeInventoryService.release(
                        TENANT_ID, STORE_ID, PRODUCT_ID, 4, "SALES_ORDER", "SO-MYSQL-RELEASE"));

        assertThat(failures).isEmpty();
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT locked_quantity FROM store_product_stock
                        WHERE tenant_id = ? AND store_id = ? AND product_id = ?
                        """, Integer.class, TENANT_ID, STORE_ID, PRODUCT_ID))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM store_inventory_change_log
                        WHERE tenant_id = ? AND store_id = ? AND product_id = ?
                          AND change_type = 'RELEASE' AND biz_type = 'SALES_ORDER'
                          AND biz_no = 'SO-MYSQL-RELEASE'
                        """, Integer.class, TENANT_ID, STORE_ID, PRODUCT_ID))
                .isEqualTo(1);
    }

    @Test
    void concurrentDuplicateInventoryDeductionShouldBeIdempotent() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO store_product_stock
                    (tenant_id, store_id, product_id, quantity, locked_quantity, version)
                VALUES (?, ?, ?, 10, 0, 0)
                """, TENANT_ID, STORE_ID, PRODUCT_ID);
        TenantContextHolder.setTenantId(TENANT_ID);
        try {
            storeInventoryService.lock(
                    TENANT_ID, STORE_ID, PRODUCT_ID, 4, "SALES_ORDER", "SO-MYSQL-DEDUCT");
        } finally {
            TenantContextHolder.clear();
        }

        List<Throwable> failures = runConcurrentlyCapturingFailures(2,
                () -> storeInventoryService.deductLocked(
                        TENANT_ID, STORE_ID, PRODUCT_ID, 4,
                        "SALES_ORDER", "SO-MYSQL-DEDUCT", USER_ID));

        assertThat(failures).isEmpty();
        assertThat(jdbcTemplate.queryForMap("""
                        SELECT quantity, locked_quantity FROM store_product_stock
                        WHERE tenant_id = ? AND store_id = ? AND product_id = ?
                        """, TENANT_ID, STORE_ID, PRODUCT_ID))
                .containsEntry("quantity", 6)
                .containsEntry("locked_quantity", 0);
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM store_inventory_change_log
                        WHERE tenant_id = ? AND store_id = ? AND product_id = ?
                          AND change_type = 'DEDUCT_LOCKED' AND biz_type = 'SALES_ORDER'
                          AND biz_no = 'SO-MYSQL-DEDUCT'
                        """, Integer.class, TENANT_ID, STORE_ID, PRODUCT_ID))
                .isEqualTo(1);
    }

    @Test
    void concurrentWholeOrderRefundCreationShouldCreateOnlyOneActiveRequest() throws Exception {
        insertOrder("SO-MYSQL-REFUND", "COMPLETED", "SUCCESS");
        RefundCreateDTO dto = new RefundCreateDTO();
        dto.setOrderNo("SO-MYSQL-REFUND");
        dto.setRefundType("REFUND_ONLY");
        dto.setRefundAmount(new BigDecimal("20.00"));
        dto.setReason("并发退款回归");

        List<Throwable> failures = runConcurrentlyCapturingFailures(2,
                () -> refundApplicationService.createRefund(USER_ID, TENANT_ID, dto));

        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst()).isInstanceOf(BusinessException.class);
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM refund_application
                        WHERE tenant_id = ? AND order_no = ? AND refund_status = 'PENDING'
                        """, Integer.class, TENANT_ID, "SO-MYSQL-REFUND"))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM after_sale_action
                        WHERE tenant_id = ? AND action = 'USER_APPLY'
                        """, Integer.class, TENANT_ID))
                .isEqualTo(1);
    }

    @Test
    void concurrentDuplicateMessageShouldExecuteBusinessOnlyOnce() throws Exception {
        String messageId = "payment.v1.order.paid:SO-MYSQL-MQ";
        String queueName = "payment.v1.order.paid";
        AtomicInteger businessExecutions = new AtomicInteger();

        List<Throwable> failures = runConcurrentlyCapturingFailures(2, () -> {
            MessageClaim claim = messageIdempotentService.tryClaim(
                    messageId, queueName, "{\"bizNo\":\"SO-MYSQL-MQ\"}", "MySqlConcurrencyTest");
            if (claim.result() != MessageClaimResult.ACQUIRED) {
                return;
            }
            businessExecutions.incrementAndGet();
            messageIdempotentService.recordSuccess(
                    messageId, queueName, "{\"bizNo\":\"SO-MYSQL-MQ\"}",
                    "MySqlConcurrencyTest", claim.token());
        });

        assertThat(failures).isEmpty();
        assertThat(businessExecutions).hasValue(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM message_idempotent WHERE message_id = ?", Integer.class, messageId))
                .isEqualTo(1);
    }

    @Test
    void concurrentFailedMessageRetryShouldHaveExactlyOneWinner() throws Exception {
        String messageId = "payment.v1.order.paid:SO-MYSQL-MQ-RETRY";
        String queueName = "payment.v1.order.paid";
        jdbcTemplate.update("""
                INSERT INTO message_idempotent
                    (message_id, queue_name, message_body, consumer_name, status, retry_count)
                VALUES (?, ?, '{}', 'PreviousConsumer', 2, 0)
                """, messageId, queueName);

        List<MessageClaim> claims = runConcurrently(List.of(
                () -> messageIdempotentService.tryClaim(
                        messageId, queueName, "{}", "RetryConsumer"),
                () -> messageIdempotentService.tryClaim(
                        messageId, queueName, "{}", "RetryConsumer")));

        assertThat(claims).extracting(MessageClaim::result).containsExactlyInAnyOrder(
                MessageClaimResult.ACQUIRED, MessageClaimResult.IN_PROGRESS);
        assertThat(jdbcTemplate.queryForMap("""
                        SELECT status, retry_count, consumer_name
                        FROM message_idempotent WHERE message_id = ?
                        """, messageId))
                .containsEntry("status", 0)
                .containsEntry("retry_count", 1)
                .containsEntry("consumer_name", "RetryConsumer");
    }

    @Test
    void staleMessageClaimShouldBeReclaimedWithoutAllowingOldWorkerToFinish() {
        String messageId = "payment.v1.order.paid:SO-MYSQL-MQ-STALE";
        String queueName = "payment.v1.order.paid";
        jdbcTemplate.update("""
                INSERT INTO message_idempotent
                    (message_id, queue_name, message_body, consumer_name, status, retry_count,
                     error_message, updated_time)
                VALUES (?, ?, '{}', 'OldConsumer', 0, 0, 'CLAIM:old-token',
                        DATE_SUB(NOW(), INTERVAL 10 MINUTE))
                """, messageId, queueName);

        MessageClaim claim = messageIdempotentService.tryClaim(
                messageId, queueName, "{}", "RecoveryConsumer");

        assertThat(claim.result()).isEqualTo(MessageClaimResult.ACQUIRED);
        assertThat(claim.token()).isNotBlank().isNotEqualTo("old-token");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> messageIdempotentService.recordSuccess(
                        messageId, queueName, "{}", "OldConsumer", "old-token"));
        messageIdempotentService.recordSuccess(
                messageId, queueName, "{}", "RecoveryConsumer", claim.token());
        assertThat(jdbcTemplate.queryForMap("""
                        SELECT status, retry_count, consumer_name, error_message
                        FROM message_idempotent WHERE message_id = ?
                        """, messageId))
                .containsEntry("status", 1)
                .containsEntry("retry_count", 1)
                .containsEntry("consumer_name", "RecoveryConsumer")
                .containsEntry("error_message", null);
    }

    private long insertOrder(String orderNo, String orderStatus, String payStatus) {
        jdbcTemplate.update("""
                INSERT INTO sales_order
                    (order_no, tenant_id, platform_user_id, order_status, pay_status,
                     total_amount, payable_amount, subject, source, wallet_strategy,
                     store_id, fulfillment_mode, deleted)
                VALUES (?, ?, ?, ?, ?, 100.00, 100.00, 'MySQL concurrency test', 'APP',
                        'NO_WALLET', ?, 'STORE_PICKUP', 0)
                """, orderNo, TENANT_ID, USER_ID, orderStatus, payStatus, STORE_ID);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sales_order WHERE order_no = ?", Long.class, orderNo);
    }

    private <T> List<T> runConcurrently(List<Callable<T>> tasks) throws Exception {
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
            List<Future<T>> futures = tasks.stream()
                    .map(task -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("concurrent test start timed out");
                        }
                        TenantContextHolder.setTenantId(TENANT_ID);
                        try {
                            return task.call();
                        } finally {
                            TenantContextHolder.clear();
                        }
                    }))
                    .toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> results = new ArrayList<>(tasks.size());
            for (Future<T> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
            return results;
        }
    }

    private List<Throwable> runConcurrentlyCapturingFailures(int taskCount, ThrowingRunnable action)
            throws Exception {
        List<Callable<Throwable>> tasks = java.util.stream.IntStream.range(0, taskCount)
                .mapToObj(index -> (Callable<Throwable>) () -> {
                    try {
                        action.run();
                        return null;
                    } catch (Throwable throwable) {
                        return throwable;
                    }
                })
                .toList();
        return runConcurrently(tasks).stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for MySQL integration tests");
        }
        return value;
    }

    private static URI validateIsolatedDatabaseUrl(String databaseUrl) {
        if (!databaseUrl.startsWith("jdbc:")) {
            throw new IllegalStateException("MYSQL_IT_URL must be a JDBC URL");
        }
        URI uri;
        try {
            uri = URI.create(databaseUrl.substring("jdbc:".length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MYSQL_IT_URL is invalid", exception);
        }
        if (!"mysql".equalsIgnoreCase(uri.getScheme())
                || !"/payment_mysql_it".equals(uri.getPath())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            throw new IllegalStateException("MYSQL_IT_URL must target the isolated payment_mysql_it database");
        }
        return uri;
    }

    private static void recreateIsolatedDatabase(URI databaseUri, String username, String password) {
        String serverUrl;
        try {
            serverUrl = "jdbc:" + new URI(
                    databaseUri.getScheme(), null, databaseUri.getHost(), databaseUri.getPort(),
                    "/", databaseUri.getQuery(), null);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build isolated MySQL server URL", exception);
        }
        try (Connection connection = DriverManager.getConnection(serverUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS payment_mysql_it");
            statement.execute("CREATE DATABASE payment_mysql_it CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to recreate isolated MySQL integration database", exception);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.payment.mapper")
    @Import({MyBatisPlusConfig.class, StoreInventoryServiceImpl.class,
            RefundApplicationServiceImpl.class, MessageIdempotentServiceImpl.class, TestDependencies.class})
    static class TestApplication {
    }

    @TestConfiguration
    static class TestDependencies {
        @Bean
        UserNotificationService userNotificationService() {
            return mock(UserNotificationService.class);
        }

        @Bean
        OrderDeliveryService orderDeliveryService() {
            return mock(OrderDeliveryService.class);
        }

        @Bean
        RefundService refundService() {
            return mock(RefundService.class);
        }

        @Bean
        MerchantStoreScopeService merchantStoreScopeService() {
            return mock(MerchantStoreScopeService.class);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
