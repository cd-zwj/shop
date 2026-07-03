package com.payment.enums;

/**
 * 支付延迟回调动作枚举。
 *
 * 定义当支付回调到达时订单已处于终态（超时/取消/关闭）
 * 应采取的补偿动作。
 */
public enum PaymentLateCallbackActionEnum {
    /** 标记支付成功：适用于可恢复的业务场景（如充值），将订单恢复为成功状态 */
    MARK_SUCCESS,
    /** 触发退款：适用于不可恢复的场景（如销售订单已取消），发起退款流程 */
    TRIGGER_REFUND,
    /** 人工审核：适用于无法自动判断的异常场景，标记为待人工处理 */
    MANUAL_REVIEW
}
