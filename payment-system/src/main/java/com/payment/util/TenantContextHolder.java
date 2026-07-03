package com.payment.util;

import cn.dev33.satoken.session.SaSession;
import com.payment.config.AuthStpKit;

/**
 * 租户上下文 - 适配 Sa-Token 多账号体系。
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
     * 获取租户ID。
     * 优先从ThreadLocal获取(手动设置优先)，其次从当前 Sa-Token Session 获取。
     */
    public static Long getTenantId() {
        Long tid = tenantIdHolder.get();
        if (tid != null) {
            return tid;
        }
        SaSession session = currentSession();
        if (session == null) {
            return null;
        }
        try {
            return session.getLong("tenantId");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清除所有租户信息。
     */
    public static void clear() {
        tenantIdHolder.remove();
    }

    private static SaSession currentSession() {
        try {
            if (AuthStpKit.MERCHANT.isLogin()) {
                return AuthStpKit.MERCHANT.getSession();
            }
            if (AuthStpKit.ADMIN.isLogin()) {
                return AuthStpKit.ADMIN.getSession();
            }
            if (AuthStpKit.PLATFORM.isLogin()) {
                return AuthStpKit.PLATFORM.getSession();
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
