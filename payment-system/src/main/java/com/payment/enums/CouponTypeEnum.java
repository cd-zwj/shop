package com.payment.enums;

/**
 * 优惠券类型枚举。
 *
 * 定义优惠券的折扣计算方式。
 */
public enum CouponTypeEnum {
    /** 满减券：满足指定金额条件后减免固定金额 */
    FULL_REDUCTION,
    /** 折扣券：按指定折扣比例打折（如 8 折） */
    DISCOUNT_RATE,
    /** 无门槛券：无消费金额限制，直接减免固定金额 */
    NO_THRESHOLD,
    /** 充值赠送券：充值时额外赠送金额或权益 */
    RECHARGE_GIFT
}
