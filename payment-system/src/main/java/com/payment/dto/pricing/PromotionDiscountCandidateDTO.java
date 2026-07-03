package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 活动优惠候选项数据传输对象，用于订单定价引擎计算优惠时作为营销活动输入参数。
 */
@Data
public class PromotionDiscountCandidateDTO {
    /** 营销活动 ID */
    private Long activityId;
    /** 活动规则 ID */
    private Long activityRuleId;
    /** 优惠类型（如 FIXED-满减、DISCOUNT-折扣） */
    private String discountType;
    /** 优惠金额 */
    private BigDecimal discountAmount;
    /** 优惠规则快照 JSON（记录计算时的规则状态） */
    private String ruleSnapshotJson;
}
