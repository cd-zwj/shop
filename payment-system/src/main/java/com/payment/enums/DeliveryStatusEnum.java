package com.payment.enums;

/**
 * 订单项 / 交付记录的交付状态枚举。
 *
 * 与 OrderStatusEnum / PayStatusEnum 解耦：同一个订单可以已支付但仍待交付，
 * 同一个订单内不同商品可以处于不同的自提凭证状态。
 */
public enum DeliveryStatusEnum {
    /** 待生成自提凭证 */
    PENDING,
    /** 自提凭证处理中 */
    DELIVERING,
    /** 自提凭证已生成，用户可见 */
    DELIVERED,
    /** 到店领取已留痕 */
    CONFIRMED,
    /** 交付失败：等死信队列重试或人工介入 */
    FAILED,
    /** 已撤销：退款回收 */
    REVOKED,
    /** 撤销失败，需要人工介入 */
    REVOKE_FAILED
}
