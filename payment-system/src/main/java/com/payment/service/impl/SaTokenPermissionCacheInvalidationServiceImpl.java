package com.payment.service.impl;

import cn.dev33.satoken.session.SaSession;
import com.payment.config.AuthStpKit;
import com.payment.service.PermissionCacheInvalidationService;
import com.payment.util.AuthLoginIdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sa-Token 权限缓存失效服务实现类。
 * <p>当用户角色或权限发生变更时，遍历平台端、商户端、管理端三个 StpLogic
 * 清除对应会话中的 roles 和 permissions 缓存，确保下次请求能加载最新权限。</p>
 */
@Service
public class SaTokenPermissionCacheInvalidationServiceImpl implements PermissionCacheInvalidationService {

    public static final String PERMISSIONS_CACHE_KEY = "permissions";
    public static final String ROLES_CACHE_KEY = "roles";

    private static final Logger log = LoggerFactory.getLogger(SaTokenPermissionCacheInvalidationServiceImpl.class);

    /**
     * 使指定用户的权限缓存失效，遍历三个认证端（平台端、商户端、管理端）清除会话中的 roles 和 permissions 缓存。
     *
     * @param userId 用户ID
     */
    @Override
    public void invalidateUser(Long userId) {
        clear(AuthStpKit.PLATFORM, AuthLoginIdHelper.platform(userId));
        clear(AuthStpKit.MERCHANT, AuthLoginIdHelper.merchant(userId));
        clear(AuthStpKit.ADMIN, AuthLoginIdHelper.admin(userId));
    }

    private void clear(cn.dev33.satoken.stp.StpLogic stpLogic, String loginId) {
        try {
            SaSession session = stpLogic.getSessionByLoginId(loginId, false);
            if (session != null) {
                session.delete(PERMISSIONS_CACHE_KEY);
                session.delete(ROLES_CACHE_KEY);
            }
        } catch (Exception e) {
            log.warn("permission_cache_invalidation_failed loginId={} type={}", loginId, stpLogic.getLoginType(), e);
        }
    }
}
