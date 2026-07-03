package com.payment.service;

/**
 * 权限缓存失效服务接口。
 *
 * <p>当用户的权限或角色发生变更时，清除已缓存的权限信息，
 * 确保下次请求时从数据库重新加载最新的权限数据。
 * 通常在权限分配、角色变更、用户状态切换等操作后调用。</p>
 */
public interface PermissionCacheInvalidationService {

    /**
     * 清理指定用户已缓存的权限与角色信息。
     *
     * <p>删除Redis中该用户的权限缓存，下次鉴权时将从数据库重新加载。</p>
     *
     * @param userId 平台用户ID
     */
    void invalidateUser(Long userId);
}
