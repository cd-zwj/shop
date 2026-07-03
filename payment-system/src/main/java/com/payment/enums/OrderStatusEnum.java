package com.payment.enums;

/**
 * 订单状态枚举。
 *
 * 描述销售订单从创建到完成（或取消/关闭）的生命周期状态。
 */
public enum OrderStatusEnum {
    /** 已创建：订单刚生成，等待用户支付 */
    CREATED,
    /** 已支付：用户完成支付，等待后续交付或处理 */
    PAID,
    /** 已取消：用户或系统主动取消订单 */
    CANCELLED,
    /** 已关闭：订单超时未支付或由平台关闭 */
    CLOSED
}
