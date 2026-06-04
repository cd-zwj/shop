package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单优惠快照写入计划。
 */
@Data
public class DiscountSnapshotPlanVO {
    private Long activityId;
    private Long activityRuleId;
    private Long userCouponId;
    private Long couponTemplateId;
    private String discountSource;
    private String discountType;
    private BigDecimal discountAmount;
    private String ruleSnapshotJson;
}
