package com.payment.enums;

/**
 * 平台登录方式枚举。
 *
 * 定义管理端 / 商户端支持的登录认证方式。
 */
public enum PlatformLoginTypeEnum {
    /** 密码登录：使用账号密码认证 */
    PASSWORD,
    /** 短信验证码登录：通过手机短信验证码认证 */
    SMS,
    /** 第三方登录：通过微信、支付宝等第三方 OAuth 认证 */
    THIRD_PARTY,
    /** 邮箱登录：通过邮箱验证码认证 */
    EMAIL
}
