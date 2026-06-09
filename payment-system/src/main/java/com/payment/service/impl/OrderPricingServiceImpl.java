package com.payment.service.impl;

import com.payment.dto.pricing.CouponDiscountCandidateDTO;
import com.payment.dto.pricing.DiscountSnapshotPlanVO;
import com.payment.dto.pricing.OrderPricingItemDTO;
import com.payment.dto.pricing.OrderPricingRequestDTO;
import com.payment.dto.pricing.OrderPricingResultVO;
import com.payment.dto.pricing.PointsHoldPlanVO;
import com.payment.dto.pricing.PromotionDiscountCandidateDTO;
import com.payment.enums.CouponTypeEnum;
import com.payment.enums.DiscountSourceEnum;
import com.payment.enums.PointsDeductStatusEnum;
import com.payment.enums.UserCouponStatusEnum;
import com.payment.service.OrderPricingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 订单定价服务实现类。
 */
@Service
public class OrderPricingServiceImpl implements OrderPricingService {

    private static final BigDecimal DEFAULT_POINT_AMOUNT = new BigDecimal("0.01");

    @Override
    public OrderPricingResultVO calculate(OrderPricingRequestDTO request) {
        BigDecimal totalAmount = scale(multiplyItems(request.getItems()));
        OrderPricingResultVO result = new OrderPricingResultVO();
        result.setTotalAmount(totalAmount);

        // 根据叠加策略决定计算顺序
        boolean couponCanStack = request.getSelectedCoupon() != null
                && Boolean.TRUE.equals(request.getSelectedCoupon().getCanStackOtherCoupon());

        BigDecimal activityDiscount;
        BigDecimal couponDiscount;

        if (couponCanStack) {
            // 可叠加：先活动再券，两者叠加
            activityDiscount = applyActivities(request.getPromotionCandidates(), result);
            couponDiscount = applyCoupon(request.getSelectedCoupon(),
                    totalAmount.subtract(activityDiscount), activityDiscount, result);
        } else {
            // 券与活动互斥：两者都有折扣时取更大的
            activityDiscount = applyActivities(request.getPromotionCandidates(), result);
            couponDiscount = applyCoupon(request.getSelectedCoupon(),
                    totalAmount.subtract(activityDiscount), activityDiscount, result);
            if (activityDiscount.compareTo(BigDecimal.ZERO) > 0
                    && couponDiscount.compareTo(BigDecimal.ZERO) > 0) {
                // 两者都有折扣，取更大的，丢弃较小的
                if (couponDiscount.compareTo(activityDiscount) >= 0) {
                    removeLastActivitySnapshots(result, request.getPromotionCandidates() == null
                            ? 0 : request.getPromotionCandidates().size());
                    activityDiscount = BigDecimal.ZERO;
                } else {
                    removeLastCouponSnapshot(result);
                    couponDiscount = BigDecimal.ZERO;
                }
            }
        }

        BigDecimal pointsDeduct = calculatePointsDeduct(request,
                totalAmount.subtract(activityDiscount).subtract(couponDiscount));

        result.setActivityDiscountAmount(scale(activityDiscount));
        result.setCouponDiscountAmount(scale(couponDiscount));
        result.setPointsDeductAmount(scale(pointsDeduct));
        result.setPayableAmount(scale(nonNegative(
                totalAmount.subtract(activityDiscount).subtract(couponDiscount).subtract(pointsDeduct))));
        result.setPointsPlan(buildPointsPlan(request, pointsDeduct));
        return result;
    }

    private BigDecimal multiplyItems(List<OrderPricingItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> safe(item.getUnitPrice()).multiply(BigDecimal.valueOf(defaultQuantity(item))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int defaultQuantity(OrderPricingItemDTO item) {
        return item.getQuantity() == null || item.getQuantity() <= 0 ? 0 : item.getQuantity();
    }

    private BigDecimal applyActivities(List<PromotionDiscountCandidateDTO> candidates, OrderPricingResultVO result) {
        return applyActivities(candidates, result, null);
    }

    /**
     * 应用活动折扣。当 eligibleAmount 非空时，对每个活动折扣做上限裁剪。
     */
    private BigDecimal applyActivities(List<PromotionDiscountCandidateDTO> candidates,
                                       OrderPricingResultVO result, BigDecimal eligibleAmount) {
        if (candidates == null || candidates.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PromotionDiscountCandidateDTO candidate : candidates) {
            BigDecimal discount = scale(nonNegative(candidate.getDiscountAmount()));
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            // 如果指定了 eligibleAmount，折扣不能超过该金额（扣除已累计折扣后）
            if (eligibleAmount != null) {
                discount = discount.min(eligibleAmount.subtract(total).max(BigDecimal.ZERO));
            }
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            total = total.add(discount);
            DiscountSnapshotPlanVO snapshot = new DiscountSnapshotPlanVO();
            snapshot.setActivityId(candidate.getActivityId());
            snapshot.setActivityRuleId(candidate.getActivityRuleId());
            snapshot.setDiscountSource(DiscountSourceEnum.ACTIVITY.name());
            snapshot.setDiscountType(candidate.getDiscountType());
            snapshot.setDiscountAmount(discount);
            snapshot.setRuleSnapshotJson(candidate.getRuleSnapshotJson());
            result.getDiscountSnapshots().add(snapshot);
        }
        return scale(total);
    }

    private BigDecimal applyCoupon(CouponDiscountCandidateDTO coupon, BigDecimal orderEligibleAmount, BigDecimal activityDiscount, OrderPricingResultVO result) {
        if (coupon == null || !UserCouponStatusEnum.RECEIVED.name().equals(coupon.getCouponStatus())) {
            return BigDecimal.ZERO;
        }
        if (!Boolean.TRUE.equals(coupon.getCanStackOtherCoupon())
                && activityDiscount.compareTo(BigDecimal.ZERO) > 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal eligibleAmount = coupon.getEligibleAmount() == null
                ? orderEligibleAmount
                : coupon.getEligibleAmount().min(orderEligibleAmount);
        if (eligibleAmount.compareTo(safe(coupon.getThresholdAmount())) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = calculateCouponDiscount(coupon, eligibleAmount);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        DiscountSnapshotPlanVO snapshot = new DiscountSnapshotPlanVO();
        snapshot.setUserCouponId(coupon.getUserCouponId());
        snapshot.setCouponTemplateId(coupon.getCouponTemplateId());
        snapshot.setDiscountSource(DiscountSourceEnum.COUPON.name());
        snapshot.setDiscountType(coupon.getCouponType());
        snapshot.setDiscountAmount(discount);
        snapshot.setRuleSnapshotJson(coupon.getRuleSnapshotJson());
        result.getDiscountSnapshots().add(snapshot);
        return discount;
    }

    private BigDecimal calculateCouponDiscount(CouponDiscountCandidateDTO coupon, BigDecimal eligibleAmount) {
        if (CouponTypeEnum.DISCOUNT_RATE.name().equals(coupon.getCouponType())) {
            BigDecimal rate = coupon.getDiscountRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal rawDiscount = eligibleAmount.multiply(BigDecimal.ONE.subtract(rate));
            if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                rawDiscount = rawDiscount.min(coupon.getMaxDiscountAmount());
            }
            return scale(rawDiscount);
        }
        if (CouponTypeEnum.FULL_REDUCTION.name().equals(coupon.getCouponType())
                || CouponTypeEnum.NO_THRESHOLD.name().equals(coupon.getCouponType())
                || CouponTypeEnum.RECHARGE_GIFT.name().equals(coupon.getCouponType())) {
            return scale(nonNegative(coupon.getDiscountAmount()).min(eligibleAmount));
        }
        return BigDecimal.ZERO;
    }

    /**
     * 移除已添加的活动快照（EXCLUSIVE 互斥策略下舍弃活动折扣时使用）。
     */
    private void removeLastActivitySnapshots(OrderPricingResultVO result, int count) {
        if (result.getDiscountSnapshots() == null || count <= 0) {
            return;
        }
        for (int i = result.getDiscountSnapshots().size() - 1; i >= 0 && count > 0; i--) {
            DiscountSnapshotPlanVO snap = result.getDiscountSnapshots().get(i);
            if (DiscountSourceEnum.ACTIVITY.name().equals(snap.getDiscountSource())) {
                result.getDiscountSnapshots().remove(i);
                count--;
            }
        }
    }

    /**
     * 移除最后添加的优惠券快照（EXCLUSIVE 互斥策略下舍弃券折扣时使用）。
     */
    private void removeLastCouponSnapshot(OrderPricingResultVO result) {
        if (result.getDiscountSnapshots() == null) {
            return;
        }
        for (int i = result.getDiscountSnapshots().size() - 1; i >= 0; i--) {
            DiscountSnapshotPlanVO snap = result.getDiscountSnapshots().get(i);
            if (DiscountSourceEnum.COUPON.name().equals(snap.getDiscountSource())) {
                result.getDiscountSnapshots().remove(i);
                return;
            }
        }
    }

    private BigDecimal calculatePointsDeduct(OrderPricingRequestDTO request, BigDecimal eligibleAmount) {
        int availablePoints = defaultPoints(request.getAvailablePoints());
        int requestedPoints = defaultPoints(request.getRequestedPoints());
        if (availablePoints <= 0 || requestedPoints <= 0 || eligibleAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        int holdPoints = Math.min(availablePoints, requestedPoints);
        BigDecimal pointAmount = request.getPointAmount() == null ? DEFAULT_POINT_AMOUNT : request.getPointAmount();
        return scale(BigDecimal.valueOf(holdPoints).multiply(pointAmount).min(eligibleAmount));
    }

    private PointsHoldPlanVO buildPointsPlan(OrderPricingRequestDTO request, BigDecimal pointsDeduct) {
        PointsHoldPlanVO plan = new PointsHoldPlanVO();
        boolean needHold = pointsDeduct.compareTo(BigDecimal.ZERO) > 0;
        plan.setNeedHold(needHold);
        plan.setDeductAmount(scale(pointsDeduct));
        plan.setStatus(needHold ? PointsDeductStatusEnum.PRE_HOLD.name() : null);
        if (!needHold) {
            plan.setHoldPoints(0);
            return plan;
        }
        BigDecimal pointAmount = request.getPointAmount() == null ? DEFAULT_POINT_AMOUNT : request.getPointAmount();
        plan.setHoldPoints(pointsDeduct.divide(pointAmount, 0, RoundingMode.DOWN).intValue());
        return plan;
    }

    private int defaultPoints(Integer points) {
        return points == null || points <= 0 ? 0 : points;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        BigDecimal safeValue = safe(value);
        return safeValue.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : safeValue;
    }

    private BigDecimal scale(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }
}
