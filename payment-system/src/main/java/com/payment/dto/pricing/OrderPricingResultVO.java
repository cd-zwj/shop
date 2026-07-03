package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单定价结果视图对象，用于返回定价引擎计算后的金额明细和优惠快照。
 */
@Data
public class OrderPricingResultVO {
    /** 订单总金额（优惠前） */
    private BigDecimal totalAmount;
    /** 活动优惠抵扣金额 */
    private BigDecimal activityDiscountAmount;
    /** 优惠券抵扣金额 */
    private BigDecimal couponDiscountAmount;
    /** 积分抵扣金额 */
    private BigDecimal pointsDeductAmount;
    /** 实际应付金额 */
    private BigDecimal payableAmount;
    /** 优惠快照写入计划列表（用于持久化到订单快照表） */
    private List<DiscountSnapshotPlanVO> discountSnapshots = new ArrayList<>();
    /** 积分预占计划 */
    private PointsHoldPlanVO pointsPlan;
}
