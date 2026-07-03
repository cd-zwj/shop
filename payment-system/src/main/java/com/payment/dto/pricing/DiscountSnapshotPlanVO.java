package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单优惠快照写入计划视图对象，用于定价完成后将优惠明细持久化到订单快照表。
 */
@Data
public class DiscountSnapshotPlanVO {
    /** 营销活动 ID（活动优惠时有值） */
    private Long activityId;
    /** 营销活动规则 ID（活动优惠时有值） */
    private Long activityRuleId;
    /** 用户优惠券记录 ID（优惠券优惠时有值） */
    private Long userCouponId;
    /** 优惠券模板 ID（优惠券优惠时有值） */
    private Long couponTemplateId;
    /** 优惠来源（如 ACTIVITY-营销活动、COUPON-优惠券） */
    private String discountSource;
    /** 优惠类型（如 FIXED-满减、DISCOUNT-折扣） */
    private String discountType;
    /** 优惠金额 */
    private BigDecimal discountAmount;
    /** 优惠规则快照 JSON（记录计算时的完整规则状态） */
    private String ruleSnapshotJson;
}
