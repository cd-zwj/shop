package com.payment.util;

import com.payment.config.AuthStpKit;

/**
 * Sa-Token 多账号体系 loginId 生成与解析工具。
 */
public final class AuthLoginIdHelper {

    private static final String SEPARATOR = ":";

    private AuthLoginIdHelper() {
    }

    public static String admin(Long userId) {
        return format(AuthStpKit.ADMIN_TYPE, userId);
    }

    public static String merchant(Long platformUserId) {
        return format(AuthStpKit.MERCHANT_TYPE, platformUserId);
    }

    public static String platform(Long platformUserId) {
        return format(AuthStpKit.PLATFORM_TYPE, platformUserId);
    }

    public static Long parse(Object loginId, String loginType) {
        if (loginId == null || loginType == null || loginType.isBlank()) {
            throw new IllegalArgumentException("loginId and loginType must not be blank");
        }

        String rawLoginId = String.valueOf(loginId);
        String expectedPrefix = loginType + SEPARATOR;
        if (!rawLoginId.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("loginId does not match loginType");
        }

        return Long.valueOf(rawLoginId.substring(expectedPrefix.length()));
    }

    private static String format(String loginType, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return loginType + SEPARATOR + userId;
    }
}
