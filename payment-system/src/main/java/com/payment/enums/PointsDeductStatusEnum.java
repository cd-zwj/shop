package com.payment.enums;

/**
 * 积分扣减状态枚举。
 *
 * 描述积分在订单支付流程中的扣减状态，
 * 采用预扣-确认/释放机制保障积分安全。
 */
public enum PointsDeductStatusEnum {
    /** 预扣：下单时预先冻结积分，尚未正式扣减 */
    PRE_HOLD,
    /** 已确认：支付成功，积分正式扣减 */
    CONFIRMED,
    /** 已释放：订单取消或支付失败，预扣的积分被释放回用户账户 */
    RELEASED,
    /** 已过期：预扣超时未确认，积分自动释放 */
    EXPIRED
}
