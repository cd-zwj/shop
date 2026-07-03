package com.payment.enums;

/**
 * 营销活动类型枚举。
 *
 * 定义商户可创建的营销活动种类。
 */
public enum ActivityTypeEnum {
    /** 限时秒杀：限时限量低价抢购 */
    FLASH_SALE,
    /** 会员价：会员专享优惠价格 */
    MEMBER_PRICE,
    /** 买赠活动：买 X 件送 Y 件 */
    BUY_X_GET_Y,
    /** 满减活动：满足金额条件后减免 */
    FULL_REDUCTION,
    /** 折扣活动：按折扣比例优惠 */
    DISCOUNT_RATE
}
