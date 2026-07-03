package com.payment.enums;

/**
 * 优惠券适用范围类型枚举。
 *
 * 定义优惠券可使用的范围限制。
 */
public enum CouponScopeTypeEnum {
    /** 全租户通用：适用于指定商户下的所有商品 */
    TENANT,
    /** 指定商品：仅适用于特定商品 */
    PRODUCT,
    /** 指定分类：适用于指定商品分类下的所有商品 */
    CATEGORY,
    /** 指定用户：仅限指定用户使用（定向发放） */
    USER
}
