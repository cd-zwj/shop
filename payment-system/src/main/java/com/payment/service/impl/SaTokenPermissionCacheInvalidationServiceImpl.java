package com.payment.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.payment.service.PermissionCacheInvalidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SaTokenPermissionCacheInvalidationServiceImpl implements PermissionCacheInvalidationService {

    public static final String PERMISSIONS_CACHE_KEY = "permissions";
    public static final String ROLES_CACHE_KEY = "roles";

    private static final Logger log = LoggerFactory.getLogger(SaTokenPermissionCacheInvalidationServiceImpl.class);

    @Override
    public void invalidateUser(Long userId) {
        try {
            SaSession session = StpUtil.getSessionByLoginId(userId, false);
            if (session != null) {
                session.delete(PERMISSIONS_CACHE_KEY);
                session.delete(ROLES_CACHE_KEY);
            }
        } catch (Exception e) {
            log.warn("permission_cache_invalidation_failed userId={}", userId, e);
        }
    }
}
