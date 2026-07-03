package com.payment.enums;

/**
 * 钱包类型枚举。
 *
 * 区分平台级统一钱包和商户级钱包两种账户类型。
 */
public enum WalletTypeEnum {
    /** 统一钱包：平台级全局钱包，用户余额跨商户通用 */
    UNIFIED,
    /** 商户钱包：商户级独立钱包，余额仅限该商户内使用 */
    MERCHANT
}
