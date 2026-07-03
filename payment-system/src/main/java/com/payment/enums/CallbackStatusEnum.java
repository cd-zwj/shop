package com.payment.enums;

/**
 * 回调状态枚举。
 *
 * 描述支付回调通知发送给商户系统的状态。
 */
public enum CallbackStatusEnum {
    /** 未回调：尚未向商户系统发送回调通知 */
    NOT_CALLBACK,
    /** 回调成功：已成功通知商户系统 */
    CALLBACK_SUCCESS,
    /** 回调失败：通知商户系统失败，待重试 */
    CALLBACK_FAILED
}
