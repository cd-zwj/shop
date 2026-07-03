package com.payment.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import com.payment.config.AuthStpKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SaTokenPermissionCacheInvalidationServiceImplTest {

    private StpLogic originalPlatform;
    private StpLogic originalMerchant;
    private StpLogic originalAdmin;

    @BeforeEach
    void saveOriginals() throws Exception {
        originalPlatform = AuthStpKit.PLATFORM;
        originalMerchant = AuthStpKit.MERCHANT;
        originalAdmin = AuthStpKit.ADMIN;
    }

    @AfterEach
    void restoreOriginals() throws Exception {
        setStaticField(AuthStpKit.class, "PLATFORM", originalPlatform);
        setStaticField(AuthStpKit.class, "MERCHANT", originalMerchant);
        setStaticField(AuthStpKit.class, "ADMIN", originalAdmin);
    }

    @Test
    void invalidateUserShouldDeletePermissionAndRoleCaches() throws Exception {
        SaSession session = mock(SaSession.class);
        StpLogic platformMock = mock(StpLogic.class);
        StpLogic merchantMock = mock(StpLogic.class);
        StpLogic adminMock = mock(StpLogic.class);

        when(platformMock.getSessionByLoginId("platform:100", false)).thenReturn(session);
        when(merchantMock.getSessionByLoginId(anyString(), anyBoolean())).thenReturn(null);
        when(adminMock.getSessionByLoginId(anyString(), anyBoolean())).thenReturn(null);

        setStaticField(AuthStpKit.class, "PLATFORM", platformMock);
        setStaticField(AuthStpKit.class, "MERCHANT", merchantMock);
        setStaticField(AuthStpKit.class, "ADMIN", adminMock);

        SaTokenPermissionCacheInvalidationServiceImpl service = new SaTokenPermissionCacheInvalidationServiceImpl();
        service.invalidateUser(100L);

        verify(session).delete(SaTokenPermissionCacheInvalidationServiceImpl.PERMISSIONS_CACHE_KEY);
        verify(session).delete(SaTokenPermissionCacheInvalidationServiceImpl.ROLES_CACHE_KEY);
        verify(platformMock).getSessionByLoginId("platform:100", false);
        verify(merchantMock).getSessionByLoginId("merchant:100", false);
        verify(adminMock).getSessionByLoginId("admin:100", false);
    }

    @Test
    void invalidateUserShouldSkipWhenSessionDoesNotExist() throws Exception {
        StpLogic platformMock = mock(StpLogic.class);
        StpLogic merchantMock = mock(StpLogic.class);
        StpLogic adminMock = mock(StpLogic.class);

        when(platformMock.getSessionByLoginId(anyString(), anyBoolean())).thenReturn(null);
        when(merchantMock.getSessionByLoginId(anyString(), anyBoolean())).thenReturn(null);
        when(adminMock.getSessionByLoginId(anyString(), anyBoolean())).thenReturn(null);

        setStaticField(AuthStpKit.class, "PLATFORM", platformMock);
        setStaticField(AuthStpKit.class, "MERCHANT", merchantMock);
        setStaticField(AuthStpKit.class, "ADMIN", adminMock);

        SaTokenPermissionCacheInvalidationServiceImpl service = new SaTokenPermissionCacheInvalidationServiceImpl();
        assertDoesNotThrow(() -> service.invalidateUser(100L));
    }

    @Test
    void invalidateUserShouldNotBreakCallerWhenSaTokenFails() throws Exception {
        StpLogic platformMock = mock(StpLogic.class);
        StpLogic merchantMock = mock(StpLogic.class);
        StpLogic adminMock = mock(StpLogic.class);

        when(platformMock.getSessionByLoginId(anyString(), anyBoolean()))
                .thenThrow(new IllegalStateException("Sa-Token unavailable"));
        when(merchantMock.getSessionByLoginId(anyString(), anyBoolean())).thenReturn(null);
        when(adminMock.getSessionByLoginId(anyString(), anyBoolean())).thenReturn(null);

        setStaticField(AuthStpKit.class, "PLATFORM", platformMock);
        setStaticField(AuthStpKit.class, "MERCHANT", merchantMock);
        setStaticField(AuthStpKit.class, "ADMIN", adminMock);

        SaTokenPermissionCacheInvalidationServiceImpl service = new SaTokenPermissionCacheInvalidationServiceImpl();
        assertDoesNotThrow(() -> service.invalidateUser(100L));
    }

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        // Java 21: use sun.misc.Unsafe to modify final static fields
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        Object base = unsafe.staticFieldBase(field);
        long offset = unsafe.staticFieldOffset(field);
        unsafe.putObject(base, offset, value);
    }
}
