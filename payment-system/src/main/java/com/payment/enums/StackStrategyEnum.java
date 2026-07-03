package com.payment.enums;

/**
 * 优惠券与活动叠加策略枚举。
 *
 * 定义当订单同时满足优惠券和营销活动条件时的叠加计算规则。
 */
public enum StackStrategyEnum {
    /** 互斥：券与活动不可同时使用，取优惠更大的一方 */
    EXCLUSIVE,
    /** 可叠加：券与活动可同时生效，分别计算优惠 */
    STACKABLE,
    /** 券优先：先计算优惠券折扣，再计算活动折扣 */
    COUPON_FIRST,
    /** 活动优先：先计算活动折扣，再计算优惠券折扣 */
    ACTIVITY_FIRST;

    /**
     * 从字符串解析叠加策略，为空或 "NONE" 时返回默认的互斥策略。
     *
     * @param value 策略名称字符串
     * @return 对应的枚举值，无效值时返回 {@link #EXCLUSIVE}
     */
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
