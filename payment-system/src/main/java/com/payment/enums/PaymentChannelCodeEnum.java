package com.payment.enums;

/**
 * 支付渠道编码枚举。
 *
 * 标识不同的第三方支付渠道，用于支付路由和策略选择。
 */
public enum PaymentChannelCodeEnum {
    /** 支付宝网页支付：PC 端支付宝扫码 / 网页收银台 */
    ALIPAY_PAGE,
    /** 外部服务商支付：通过第三方支付服务商接入的支付方式 */
    EXT_PROVIDER
}
