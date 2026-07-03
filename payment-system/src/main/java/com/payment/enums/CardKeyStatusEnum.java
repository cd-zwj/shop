package com.payment.enums;

/**
 * 卡密库存状态枚举。
 *
 * 描述卡密（兑换码 / 序列号）在库存中的生命周期状态。
 */
public enum CardKeyStatusEnum {
    /** 可交付：卡密在库存中，可用于售卖 */
    AVAILABLE,
    /** 已售出：卡密已绑定到订单项并交付给用户 */
    USED,
    /** 已退回：退款或撤销后作废，不再二次销售 */
    RETURNED,
    /** 已停用：商家手动停用，不再售卖 */
    DISABLED
}
