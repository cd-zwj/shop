package com.payment.util;

/**
 * 租户上下文 - 使用ThreadLocal保存当前租户信息
 */
public class TenantContextHolder {
    
    private static final ThreadLocal<Long> tenantIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> tenantCodeHolder = new ThreadLocal<>();
    
    /**
     * 设置租户ID
     */
    public static void setTenantId(Long tenantId) {
        tenantIdHolder.set(tenantId);
    }
    
    /**
     * 获取租户ID
     */
    public static Long getTenantId() {
        return tenantIdHolder.get();
    }
    
    /**
     * 设置租户编码
     */
    public static void setTenantCode(String tenantCode) {
        tenantCodeHolder.set(tenantCode);
    }
    
    /**
     * 获取租户编码
     */
    public static String getTenantCode() {
        return tenantCodeHolder.get();
    }
    
    /**
     * 清除所有租户信息（防止内存泄漏）
     */
    public static void clear() {
        tenantIdHolder.remove();
        tenantCodeHolder.remove();
    }
}

