package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券优惠候选项数据传输对象，用于订单定价引擎计算优惠时作为优惠券输入参数。
 */
@Data
public class CouponDiscountCandidateDTO {
    /** 用户优惠券记录 ID */
    private Long userCouponId;
    /** 优惠券模板 ID */
    private Long couponTemplateId;
    /** 优惠券类型（如 FIXED-满减、DISCOUNT-折扣） */
    private String couponType;
    /** 优惠券状态 */
    private String couponStatus;
    /** 订单中符合优惠券适用范围的金额 */
    private BigDecimal eligibleAmount;
    /** 使用门槛金额 */
    private BigDecimal thresholdAmount;
    /** 固定减免金额 */
    private BigDecimal discountAmount;
    /** 折扣率 */
    private BigDecimal discountRate;
    /** 最大减免金额（折扣封顶） */
    private BigDecimal maxDiscountAmount;
    /** 是否可与余额叠加使用 */
    private Boolean canStackBalance;
    /** 是否可与积分叠加使用 */
    private Boolean canStackPoints;
    /** 是否可与其他优惠券叠加使用 */
    private Boolean canStackOtherCoupon;
    /** 优惠规则快照 JSON（记录计算时的规则状态） */
    private String ruleSnapshotJson;
}
