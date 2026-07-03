package com.payment.enums;

/**
 * 退款状态枚举。
 *
 * 描述退款流程从申请到完成（或失败/关闭）的整体状态。
 */
public enum RefundStatusEnum {
    /** 已申请：用户提交退款申请，等待处理 */
    APPLIED,
    /** 处理中：退款正在向第三方支付渠道发起 */
    PROCESSING,
    /** 退款成功：第三方渠道已退款，资金已返还 */
    SUCCESS,
    /** 退款失败：第三方渠道退款失败 */
    FAIL,
    /** 已关闭：退款申请被关闭（超时、取消等原因） */
    CLOSED
}
