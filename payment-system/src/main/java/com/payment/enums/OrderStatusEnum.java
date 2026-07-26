package com.payment.enums;

/**
 * 订单状态枚举。
 *
 * 描述销售订单从创建到完成（或取消/关闭）的生命周期状态。
 */
public enum OrderStatusEnum {
    /** 已创建：订单刚生成，等待用户支付 */
    CREATED,
    /**
     * 资金已确认、等待异步支付后处理的受控中间态。
     * 外部支付回调先抢占到该状态，ORDER_PAID 消费者完成库存扣减、结算与
     * 交付事件入队后再推进到 {@link #PENDING_PREPARATION}。
     */
    PAID,
    /** 已支付，等待商户开始备货 */
    PENDING_PREPARATION,
    /** 商户正在备货 */
    PREPARING,
    /** 商户已确认备货完成，订单履约结束 */
    COMPLETED,
    /** 已取消：用户或系统主动取消订单 */
    CANCELLED,
    /** 已关闭：订单超时未支付或由平台关闭 */
    CLOSED
}
