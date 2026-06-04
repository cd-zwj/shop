package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 活动优惠候选项。
 */
@Data
public class PromotionDiscountCandidateDTO {
    private Long activityId;
    private Long activityRuleId;
    private String discountType;
    private BigDecimal discountAmount;
    private String ruleSnapshotJson;
}
