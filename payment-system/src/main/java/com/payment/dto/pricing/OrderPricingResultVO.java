package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单定价结果。
 */
@Data
public class OrderPricingResultVO {
    private BigDecimal totalAmount;
    private BigDecimal activityDiscountAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal pointsDeductAmount;
    private BigDecimal payableAmount;
    private List<DiscountSnapshotPlanVO> discountSnapshots = new ArrayList<>();
    private PointsHoldPlanVO pointsPlan;
}
