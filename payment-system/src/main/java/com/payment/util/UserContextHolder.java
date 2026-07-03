package com.payment.util;

import cn.dev33.satoken.stp.StpLogic;
import com.payment.config.AuthStpKit;

/**
 * 用户上下文 - 适配 Sa-Token 多账号体系。
 */
public class UserContextHolder {

    /**
     * 获取用户ID。
     */
    public static Long getUserId() {
        StpLogic logic = currentLogic();
        if (logic == null) {
            return null;
        }
        try {
            return AuthLoginIdHelper.parse(logic.getLoginId(), logic.getLoginType());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取用户名。
     */
    public static String getUsername() {
        StpLogic logic = currentLogic();
        if (logic == null) {
            return null;
        }
        try {
            Object username = logic.getSession().get("username");
            if (username == null) {
                username = logic.getSession().get("platformUsername");
            }
            return username == null ? null : username.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取token。
     */
    public static String getToken() {
        StpLogic logic = currentLogic();
        return logic == null ? null : logic.getTokenValue();
    }

    /**
     * 清除所有用户信息 (Sa-Token会自动管理，此处留空或用于清理ThreadLocal如果仍有混合使用)。
     */
    public static void clear() {
        // Sa-Token不需要手动清理ThreadLocal
    }

    private static StpLogic currentLogic() {
        try {
            if (AuthStpKit.MERCHANT.isLogin()) {
                return AuthStpKit.MERCHANT;
            }
            if (AuthStpKit.ADMIN.isLogin()) {
                return AuthStpKit.ADMIN;
            }
            if (AuthStpKit.PLATFORM.isLogin()) {
                return AuthStpKit.PLATFORM;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
