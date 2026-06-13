package com.payment.common;

/**
 * 租户上下文持有者
 * 使用 InheritableThreadLocal 使 @Async 子线程自动继承父线程的租户 ID。
 * 注意：线程池复用场景下仍需 TaskDecorator 兜底，此处先用 InheritableThreadLocal 覆盖常见路径。
 */
public class TenantContextHolder {

    private static final InheritableThreadLocal<Long> TENANT_ID_HOLDER = new InheritableThreadLocal<>();

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
