package com.payment.util;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 用户上下文 - 适配 Sa-Token
 */
public class UserContextHolder {

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取用户名
     */
    public static String getUsername() {
        try {
            Object username = StpUtil.getSession().get("username");
            return username != null ? username.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取token
     */
    public static String getToken() {
        return StpUtil.getTokenValue();
    }

    /**
     * 清除所有用户信息 (Sa-Token会自动管理，此处留空或用于清理ThreadLocal如果仍有混合使用)
     */
    public static void clear() {
        // Sa-Token不需要手动清理ThreadLocal
    }
}

