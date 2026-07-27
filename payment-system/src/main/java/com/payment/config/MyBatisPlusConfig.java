package com.payment.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.payment.util.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * MyBatis-Plus 插件配置。
 * <p>
 * 注册三个核心拦截器：
 * <ul>
 *   <li>{@link TenantLineInnerInterceptor} — 多租户行级隔离，自动注入 tenant_id 条件</li>
 *   <li>{@link OptimisticLockerInnerInterceptor} — 乐观锁，保障钱包余额等并发场景安全</li>
 *   <li>{@link PaginationInnerInterceptor} — 物理分页（MySQL）</li>
 * </ul>
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 平台级表不参与租户隔离。
     * <p>
     * 这些表属于全局共享数据（如系统配置、统一钱包、支付流水等），不应注入 tenant_id 条件。
     */
    private static final List<String> IGNORE_TABLES = Arrays.asList(
            "tenant",
            "sys_config",
            "platform_user",
            "platform_user_auth",
            "platform_auth_provider",
            "user_shipping_address",
            "user_notification",
            "message_idempotent",
            "tenant_employee",
            "tenant_member",
            "sys_role",
            "sys_permission",
            "sys_user_role",
            "sys_user_permission",
            "sys_role_permission",
            "login_fail_record",
            "unified_wallet_account",
            "unified_wallet_log",
            "payment_bill",
            "payment_callback_record",
            "payment_callback_failure_audit",
            "refund_order",
            "refund_record",
            "refund_callback_record",
            "refund_reconcile_task",
            "recharge_order_v1",
            "message_outbox",
            "compensation_task",
            "dead_letter_task",
            // RAG 知识库表：无 tenant_id，靠 user_id 隔离
            "rag_unit",
            "document_file",
            "ai_feedback"
    );

    /**
     * 创建 MyBatis-Plus 拦截器链。
     * <p>
     * 拦截器执行顺序：租户过滤 → 乐观锁 → 分页。
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        TenantLineInnerInterceptor tenantLine = new TenantLineInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.getTenantId();
                if (tenantId == null) {
                    return null;
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TABLES.contains(tableName.toLowerCase());
            }
        });
        interceptor.addInnerInterceptor(tenantLine);

        // 钱包账户等高并发更新场景启用乐观锁。
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
