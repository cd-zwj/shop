package com.payment.enums;

/**
 * 用户券状态枚举。
 *
 * 描述用户持有的优惠券从领取到使用（或过期）的生命周期状态。
 */
public enum UserCouponStatusEnum {
    /** 已领取：优惠券已发放到用户账户，可正常使用 */
    RECEIVED,
    /** 已锁定：用户下单时已选中该券，支付完成前暂时锁定不可重复使用 */
    LOCKED,
    /** 已使用：优惠券已被核销 */
    USED,
    /** 已释放：订单取消或支付失败后，锁定的券被释放回可用状态 */
    RELEASED,
    /** 已过期：优惠券超过有效期，自动失效 */
    EXPIRED
}
