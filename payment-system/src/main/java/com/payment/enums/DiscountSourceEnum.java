package com.payment.enums;

/**
 * 订单优惠来源枚举。
 *
 * 标识订单享受的优惠来自营销活动还是优惠券。
 */
public enum DiscountSourceEnum {
    /** 营销活动：优惠来自营销活动（如秒杀、满减活动等） */
    ACTIVITY,
    /** 优惠券：优惠来自用户使用的优惠券 */
    COUPON
}
