package com.payment.service;

/**
 * 权限缓存失效服务。
 */
public interface PermissionCacheInvalidationService {

    /**
     * 清理指定用户已缓存的权限与角色信息。
     */
    void invalidateUser(Long userId);
}
