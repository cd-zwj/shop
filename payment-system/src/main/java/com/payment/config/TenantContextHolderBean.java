package com.payment.config;

import org.springframework.stereotype.Component;

/**
 * 租户上下文 Bean，暴露当前租户 ID 给 Spring Expression Language (SpEL)。
 * <p>
 * 注册名为 {@code tenantContextHolder} 的 Bean，可在 SpEL 表达式中通过
 * {@code @tenantContextHolder.tenantId} 引用当前租户 ID。
 * </p>
 */
@Component("tenantContextHolder")
public class TenantContextHolderBean {

    /**
     * 获取当前租户 ID。
     *
     * @return 租户 ID，未设置时返回 0L
     */
    public Long getTenantId() {
        Long tenantId = com.payment.util.TenantContextHolder.getTenantId();
        return tenantId == null ? 0L : tenantId;
    }
}
