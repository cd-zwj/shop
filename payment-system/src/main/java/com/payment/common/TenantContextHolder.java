package com.payment.common;

/**
 * 租户上下文持有者
 * 用于在当前线程中存储和获取租户ID
 */
public class TenantContextHolder {
    
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置当前租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }
    
    /**
     * 获取当前租户ID
     */
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }
    
    /**
     * 清除当前租户ID
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}
