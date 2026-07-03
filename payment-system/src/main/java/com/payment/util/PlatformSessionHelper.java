package com.payment.util;

import cn.dev33.satoken.session.SaSession;
import com.payment.config.AuthStpKit;

/**
 * 平台会话辅助工具类。
 * <p>
 * 从 Sa-Token 多账号会话中提取平台用户信息（用户 ID、用户名），
 * 优先从商户端会话获取，其次从管理端会话获取，最后从 C 端用户会话获取；管理端返回 platform_user 管理员 ID。
 * </p>
 */
public final class PlatformSessionHelper {

    /** 私有构造，禁止实例化工具类 */
    private PlatformSessionHelper() {
    }

    /**
     * 获取当前平台用户 ID。
     *
     * @return 平台用户 ID
     * @throws RuntimeException 用户未登录时抛出
     */
    public static Long getPlatformUserId() {
        SaSession session = currentSession();
        Object value = session.get("platformUserId");
        if (value == null && AuthStpKit.ADMIN.isLogin()) {
            value = session.get("userId");
        }
        if (value == null) {
            throw new RuntimeException("平台用户未登录");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * 获取当前平台用户名。
     *
     * @return 用户名，未登录时返回 null
     */
    public static String getUsername() {
        SaSession session = currentSession();
        Object value = session.get("platformUsername");
        if (value == null && AuthStpKit.ADMIN.isLogin()) {
            value = session.get("username");
        }
        return value == null ? null : value.toString();
    }

    /**
     * 获取当前 Sa-Token 会话。
     * <p>
     * 优先返回商户端会话，其次返回管理端会话，最后返回 C 端用户会话。
     *
     * @return SaSession 实例
     */
    private static SaSession currentSession() {
        if (AuthStpKit.MERCHANT.isLogin()) {
            return AuthStpKit.MERCHANT.getSession();
        }
        if (AuthStpKit.ADMIN.isLogin()) {
            return AuthStpKit.ADMIN.getSession();
        }
        return AuthStpKit.PLATFORM.getSession();
    }
}
