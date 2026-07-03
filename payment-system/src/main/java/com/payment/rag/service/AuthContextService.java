package com.payment.rag.service;

import cn.dev33.satoken.stp.StpLogic;
import com.payment.config.AuthStpKit;
import com.payment.util.PlatformSessionHelper;
import com.payment.util.TenantContextHolder;
import com.payment.util.UserContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 认证上下文适配 —— 封装主系统 Sa-Token 上下文，替换 RAG 原有的独立认证。
 */
@Service
public class AuthContextService {

    /** 获取当前登录用户 ID（String 版本，兼容 RAG 原有调用）。 */
    public String getCurrentUserId() {
        Long platformUserId = tryGetPlatformUserId();
        if (platformUserId != null) {
            return platformUserId.toString();
        }
        Long uid = UserContextHolder.getUserId();
        return uid != null ? uid.toString() : null;
    }

    /**
     * 获取当前登录平台用户 ID（Long 版本）。
     * 优先从 PlatformSessionHelper 取，否则从 UserContextHolder 取。
     * 若用户未登录则抛出异常。
     */
    public Long getCurrentPlatformUserId() {
        Long platformUserId = tryGetPlatformUserId();
        if (platformUserId != null) {
            return platformUserId;
        }
        Long uid = UserContextHolder.getUserId();
        if (uid != null) {
            return uid;
        }
        throw new IllegalArgumentException("当前用户未登录");
    }

    public String getCurrentRole() {
        if (!isAnyLogin()) {
            throw new IllegalArgumentException("当前用户未登录");
        }
        if (sessionValue("merchantTenantId") != null || sessionValue("merchantEmployeeRole") != null) {
            return "merchant";
        }
        List<String> roles = getCurrentRoles();
        if (roles.contains("admin") || roles.contains("ADMIN") || sessionValue("adminUser") != null) {
            return "admin";
        }
        return "user";
    }

    public Long getCurrentTenantId() {
        Object merchantTenantId = sessionValue("merchantTenantId");
        if (merchantTenantId instanceof Number number) {
            return number.longValue();
        }
        if (merchantTenantId != null) {
            return Long.parseLong(merchantTenantId.toString());
        }
        return TenantContextHolder.getTenantId();
    }

    @SuppressWarnings("unchecked")
    public List<String> getCurrentPermissions() {
        Object permissions = sessionValue("permissions");
        if (permissions instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        try {
            return new ArrayList<>(currentStpLogic().getPermissionList());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getCurrentRoles() {
        Object roles = sessionValue("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        try {
            return new ArrayList<>(currentStpLogic().getRoleList());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * 解析用户 ID：若请求中传入 userId 则校验是否与当前登录用户一致，
     * 否则返回当前登录用户 ID。
     */
    public String resolveUserId(String requestedUserId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new IllegalArgumentException("当前用户未登录");
        }
        if (requestedUserId == null || requestedUserId.isBlank()) {
            return currentUserId;
        }
        if (!currentUserId.equals(requestedUserId)) {
            throw new IllegalArgumentException("请求中的用户标识与当前登录用户不一致");
        }
        return currentUserId;
    }

    private boolean isAnyLogin() {
        return AuthStpKit.PLATFORM.isLogin() || AuthStpKit.MERCHANT.isLogin() || AuthStpKit.ADMIN.isLogin();
    }

    private StpLogic currentStpLogic() {
        if (AuthStpKit.MERCHANT.isLogin()) return AuthStpKit.MERCHANT;
        if (AuthStpKit.ADMIN.isLogin()) return AuthStpKit.ADMIN;
        return AuthStpKit.PLATFORM;
    }

    private Long tryGetPlatformUserId() {
        try {
            return PlatformSessionHelper.getPlatformUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object sessionValue(String key) {
        try {
            return currentStpLogic().getSession().get(key);
        } catch (Exception ignored) {
            return null;
        }
    }
}