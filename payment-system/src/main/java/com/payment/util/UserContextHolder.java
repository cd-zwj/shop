package com.payment.util;

/**
 * 用户上下文 - 使用ThreadLocal保存当前登录用户信息
 */
public class UserContextHolder {
    
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> tokenHolder = new ThreadLocal<>();
    
    /**
     * 设置用户ID
     */
    public static void setUserId(Long userId) {
        userIdHolder.set(userId);
    }
    
    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return userIdHolder.get();
    }
    
    /**
     * 设置用户名
     */
    public static void setUsername(String username) {
        usernameHolder.set(username);
    }
    
    /**
     * 获取用户名
     */
    public static String getUsername() {
        return usernameHolder.get();
    }
    
    /**
     * 设置token
     */
    public static void setToken(String token) {
        tokenHolder.set(token);
    }
    
    /**
     * 获取token
     */
    public static String getToken() {
        return tokenHolder.get();
    }
    
    /**
     * 清除所有用户信息（防止内存泄漏）
     */
    public static void clear() {
        userIdHolder.remove();
        usernameHolder.remove();
        tokenHolder.remove();
    }
}

