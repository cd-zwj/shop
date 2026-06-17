package com.payment.enums;

/**
 * 订单项 / 交付记录的交付状态枚举。
 *
 * 与 OrderStatusEnum / PayStatusEnum 解耦：同一个订单可以已支付但仍待交付，
 * 同一个订单内不同 item 也可以处于不同的交付阶段（混合订单）。
 */
public enum DeliveryStatusEnum {
    /** 待交付：刚下单或支付成功但 Consumer 还没处理 */
    PENDING,
    /** 交付中：异步流程进行中（卡密发码、第三方对接等） */
    DELIVERING,
    /** 已交付：内容/卡密/单号已落库，用户可见 */
    DELIVERED,
    /** 已确认：用户主动确认收货 / 已使用 */
    CONFIRMED,
    /** 交付失败：等死信队列重试或人工介入 */
    FAILED,
    /** 已撤销：退款回收 */
    REVOKED,
    /** 撤销失败：strategy.revoke() 抛错,资源可能未真正回收,需人工介入 */
    REVOKE_FAILED
}
