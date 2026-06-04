package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券优惠候选项。
 */
@Data
public class CouponDiscountCandidateDTO {
    private Long userCouponId;
    private Long couponTemplateId;
    private String couponType;
    private String status;
    private BigDecimal eligibleAmount;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private String stackStrategy;
    private String ruleSnapshotJson;
}
