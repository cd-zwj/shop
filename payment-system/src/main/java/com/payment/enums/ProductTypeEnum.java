package com.payment.enums;

/**
 * 商品类型枚举。
 *
 * 决定下单支付成功后走哪一种 DeliveryStrategy。
 */
public enum ProductTypeEnum {
    /** 实物：支付后等待商户发货 */
    PHYSICAL,
    /** 虚拟内容：支付后立即返回内容 URL / 账号信息 */
    VIRTUAL,
    /** 卡密 / 兑换码 / 序列号 */
    CARD_KEY,
    /** 服务类：到店核销 */
    SERVICE,
    /** 订阅 / 权益包：开通后按时长有效 */
    SUBSCRIPTION
}
