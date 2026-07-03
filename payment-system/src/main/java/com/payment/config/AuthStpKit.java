package com.payment.config;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Sa-Token 多账号体系入口类。
 * <p>
 * 为平台管理端（admin）、商户端（merchant）、C 端用户（platform）分别创建独立的
 * {@link StpLogic} 实例，实现三端登录态互不干扰。
 * </p>
 */
public final class AuthStpKit {

    /** 管理端账号类型标识 */
    public static final String ADMIN_TYPE = "admin";
    /** 商户端账号类型标识 */
    public static final String MERCHANT_TYPE = "merchant";
    /** C 端用户账号类型标识 */
    public static final String PLATFORM_TYPE = "platform";

    /** 管理端 Sa-Token 逻辑实例 */
    public static final StpLogic ADMIN = new StpLogic(ADMIN_TYPE);
    /** 商户端 Sa-Token 逻辑实例 */
    public static final StpLogic MERCHANT = new StpLogic(MERCHANT_TYPE);
    /** C 端用户 Sa-Token 逻辑实例 */
    public static final StpLogic PLATFORM = new StpLogic(PLATFORM_TYPE);

    /** 私有构造，禁止实例化工具类 */
    private AuthStpKit() {
    }
}
