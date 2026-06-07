package com.payment.enums;

/**
 * 优惠券与活动叠加策略枚举。
 */
public enum StackStrategyEnum {
    /** 券与活动互斥，取优惠更大的 */
    EXCLUSIVE,
    /** 券与活动可同时生效 */
    STACKABLE,
    /** 先算券再算活动 */
    COUPON_FIRST,
    /** 先算活动再算券 */
    ACTIVITY_FIRST;

    public static StackStrategyEnum fromString(String value) {
        if (value == null || value.isEmpty() || "NONE".equals(value)) {
            return EXCLUSIVE; // 默认策略
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return EXCLUSIVE;
        }
    }
}
