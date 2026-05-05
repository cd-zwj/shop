package com.payment.util;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 租户上下文 - 适配 Sa-Token
 */
public class TenantContextHolder {

    private static final ThreadLocal<Long> tenantIdHolder = new ThreadLocal<>();

    /**
     * 设置租户ID (保留ThreadLocal以支持某些非Web场景或手动切换租户)
     */
    public static void setTenantId(Long tenantId) {
        tenantIdHolder.set(tenantId);
    }

    /**
     * 获取租户ID
     * 优先从ThreadLocal获取(手动设置优先)，其次从Sa-Token Session获取
     */
    public static Long getTenantId() {
        Long tid = tenantIdHolder.get();
        if (tid != null) {
            return tid;
        }
        try {
            // 从Session获取
            return StpUtil.getSession().getLong("tenantId");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清除所有租户信息
     */
    public static void clear() {
        tenantIdHolder.remove();
    }
}

