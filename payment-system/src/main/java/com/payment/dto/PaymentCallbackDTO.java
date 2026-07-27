package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调数据传输对象，封装第三方支付平台回调通知的核心信息。
 */
@Data
public class PaymentCallbackDTO {

    /** 平台内部支付账单编号 */
    private String billNo;

    /** 回调请求唯一标识（用于幂等处理） */
    private String callbackRequestId;

    /** 第三方交易流水号 */
    private String thirdPartyBillNo;

    /** 支付是否成功 */
    private Boolean success;

    /** 渠道是否明确返回不可重试的支付失败终态 */
    private Boolean terminalFailure;

    /** 第三方回调原始报文（JSON 字符串） */
    private String rawBody;
}
