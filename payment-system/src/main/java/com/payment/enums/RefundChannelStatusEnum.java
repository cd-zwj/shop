package com.payment.enums;

/**
 * 退款渠道状态枚举。
 *
 * 描述退款请求在第三方支付渠道（微信/支付宝等）中的处理状态。
 */
public enum RefundChannelStatusEnum {
    /** 处理中：退款请求已提交到渠道，等待处理结果 */
    PROCESSING,
    /** 退款成功：渠道已处理完成，资金已退还 */
    SUCCESS,
    /** 退款失败：渠道处理失败 */
    FAIL
}
