package com.payment.controller;

import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import com.payment.config.AuthStpKit;
import com.payment.util.AuthLoginIdHelper;

/**
 * Sa-Token helpers for MockMvc tests that need to create tokens outside a servlet request.
 */
final class SaTokenTestSupport {

    private SaTokenTestSupport() {
    }

    static String loginPlatformUser(long userId) {
        return SaTokenContextMockUtil.setMockContext(() -> {
            AuthStpKit.PLATFORM.login(AuthLoginIdHelper.platform(userId));
            AuthStpKit.PLATFORM.getSession().set("platformUserId", userId);
            return AuthStpKit.PLATFORM.getTokenValue();
        });
    }

    static void logoutPlatformUser() {
        SaTokenContextMockUtil.setMockContext(() -> {
            try {
                AuthStpKit.PLATFORM.logout();
            } catch (Exception ignored) {
                // Tests often start without a login session.
            }
        });
    }
}
