package com.payment.dto;

import lombok.Data;

/**
 * 外部支付查询结果数据传输对象，用于返回向第三方支付平台（微信/支付宝）主动查询的支付状态。
 */
@Data
public class ExternalPaymentQueryResult {

    /** 查询是否成功 */
    private boolean success;
    /** 是否已支付 */
    private boolean paid;
    /** 支付平台交易号 */
    private String providerTradeNo;
    /** 渠道交易号 */
    private String channelTradeNo;
    /** 第三方原始状态码 */
    private String rawStatus;
    /** 查询结果消息 */
    private String message;
    /** 付款方标识（如微信 OpenID） */
    private String buyer;
}
