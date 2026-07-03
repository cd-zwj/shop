package com.payment.enums;

/**
 * 优惠券归属类型枚举。
 *
 * 区分优惠券由平台还是商户创建和管理。
 */
public enum CouponOwnerTypeEnum {
    /** 平台券：由平台统一创建，所有商户通用 */
    PLATFORM,
    /** 租户券：由商户自行创建，仅限该商户下使用 */
    TENANT
}
