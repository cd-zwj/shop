package com.payment.config;

import org.springframework.stereotype.Component;

/**
 * Exposes the current tenant id to Spring Expression Language.
 */
@Component("tenantContextHolder")
public class TenantContextHolderBean {

    public Long getTenantId() {
        Long tenantId = com.payment.util.TenantContextHolder.getTenantId();
        return tenantId == null ? 0L : tenantId;
    }
}
