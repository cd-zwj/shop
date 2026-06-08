package com.payment.util;

/**
 * 用户上下文工具类
 * 用于在Controller中方便地获取当前登录用户信息
 * 注意：现在使用ThreadLocal存储，可以直接调用静态方法，无需传递request
 */
public class UserContext {

    /**
     * 从ThreadLocal中获取当前用户ID
     */
    public static Long getCurrentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }

    /**
     * 从ThreadLocal中获取当前用户名
     */
    public static String getCurrentUsername() {
        String username = UserContextHolder.getUsername();
        if (username == null) {
            throw new RuntimeException("用户未登录");
        }
        return username;
    }

    /**
     * 从ThreadLocal中获取当前token
     */
    public static String getCurrentToken() {
        return UserContextHolder.getToken();
    }

    /**
     * 从ThreadLocal中获取当前租户ID
     */
    public static Long getCurrentTenantId() {
        return TenantContextHolder.getTenantId();
    }
}
