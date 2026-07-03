package com.payment.enums;

/**
 * 支付业务类型枚举。
 *
 * 区分支付账单所关联的业务场景，
 * 用于支付回调后的业务路由处理。
 */
public enum PaymentBizTypeEnum {
    /** 销售订单：用户购买商品产生的支付 */
    SALES_ORDER,
    /** 充值：用户向钱包充值产生的支付 */
    RECHARGE
}
