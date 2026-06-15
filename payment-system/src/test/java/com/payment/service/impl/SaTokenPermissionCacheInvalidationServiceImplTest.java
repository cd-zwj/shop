package com.payment.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class SaTokenPermissionCacheInvalidationServiceImplTest {

    @Test
    void invalidateUserShouldDeletePermissionAndRoleCaches() {
        SaSession session = mock(SaSession.class);
        SaTokenPermissionCacheInvalidationServiceImpl service = new SaTokenPermissionCacheInvalidationServiceImpl();

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getSessionByLoginId(100L, false)).thenReturn(session);

            service.invalidateUser(100L);
        }

        verify(session).delete(SaTokenPermissionCacheInvalidationServiceImpl.PERMISSIONS_CACHE_KEY);
        verify(session).delete(SaTokenPermissionCacheInvalidationServiceImpl.ROLES_CACHE_KEY);
    }

    @Test
    void invalidateUserShouldSkipWhenSessionDoesNotExist() {
        SaTokenPermissionCacheInvalidationServiceImpl service = new SaTokenPermissionCacheInvalidationServiceImpl();

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getSessionByLoginId(100L, false)).thenReturn(null);

            assertDoesNotThrow(() -> service.invalidateUser(100L));
        }
    }

    @Test
    void invalidateUserShouldNotBreakCallerWhenSaTokenFails() {
        SaTokenPermissionCacheInvalidationServiceImpl service = new SaTokenPermissionCacheInvalidationServiceImpl();

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getSessionByLoginId(100L, false))
                    .thenThrow(new IllegalStateException("Sa-Token unavailable"));

            assertDoesNotThrow(() -> service.invalidateUser(100L));
        }
    }
}
