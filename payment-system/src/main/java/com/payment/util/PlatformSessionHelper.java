package com.payment.util;

import cn.dev33.satoken.stp.StpUtil;

public final class PlatformSessionHelper {

    private PlatformSessionHelper() {
    }

    public static Long getPlatformUserId() {
        Object value = StpUtil.getSession().get("platformUserId");
        if (value == null) {
            throw new RuntimeException("平台用户未登录");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    public static String getUsername() {
        Object value = StpUtil.getSession().get("platformUsername");
        return value == null ? null : value.toString();
    }
}
