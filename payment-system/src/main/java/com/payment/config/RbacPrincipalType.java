package com.payment.config;

/**
 * RBAC principal type values stored in sys_user_role and sys_user_permission.
 */
public final class RbacPrincipalType {

    public static final String ADMIN = AuthStpKit.ADMIN_TYPE;
    public static final String MERCHANT = AuthStpKit.MERCHANT_TYPE;
    public static final String PLATFORM = AuthStpKit.PLATFORM_TYPE;

    private RbacPrincipalType() {
    }

    public static String fromLoginType(String loginType) {
        if (AuthStpKit.ADMIN_TYPE.equals(loginType)) {
            return ADMIN;
        }
        if (AuthStpKit.MERCHANT_TYPE.equals(loginType)) {
            return MERCHANT;
        }
        if (AuthStpKit.PLATFORM_TYPE.equals(loginType)) {
            return PLATFORM;
        }
        throw new IllegalArgumentException("Unsupported loginType: " + loginType);
    }
}
