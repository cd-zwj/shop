package com.payment.enums;

/**
 * 卡密库存状态。
 */
public enum CardKeyStatusEnum {
    /** 可交付 */
    AVAILABLE,
    /** 已售出并绑定订单项 */
    USED,
    /** 退款/撤销后作废，不再二次销售 */
    RETURNED,
    /** 商家手动停用 */
    DISABLED
}
