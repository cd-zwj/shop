package com.payment.service.impl;

import com.payment.dto.pricing.CouponDiscountCandidateDTO;
import com.payment.dto.pricing.OrderPricingItemDTO;
import com.payment.dto.pricing.OrderPricingRequestDTO;
import com.payment.dto.pricing.OrderPricingResultVO;
import com.payment.dto.pricing.PromotionDiscountCandidateDTO;
import com.payment.enums.CouponTypeEnum;
import com.payment.enums.UserCouponStatusEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderPricingServiceImplTest {

    @Test
    void calculateShouldFreezeActivityAndCouponDiscountsAndCreatePointsHoldPlan() {
        OrderPricingServiceImpl service = new OrderPricingServiceImpl();

        OrderPricingRequestDTO request = new OrderPricingRequestDTO();
        request.setTenantId(9L);
        request.setPlatformUserId(100L);
        request.setOrderNo("SO1001");
        request.setItems(List.of(
                item(1L, "饮品", "20.00", 2),
                item(2L, "甜点", "10.00", 1)
        ));
        request.setPromotionCandidates(List.of(activity(11L, 21L, "5.00")));
        request.setSelectedCoupon(coupon(301L, 201L, CouponTypeEnum.FULL_REDUCTION.name(), "30.00", "8.00", null));
        request.setAvailablePoints(1000);
        request.setRequestedPoints(300);

        OrderPricingResultVO result = service.calculate(request);

        assertEquals(new BigDecimal("50.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("5.00"), result.getActivityDiscountAmount());
        assertEquals(new BigDecimal("8.00"), result.getCouponDiscountAmount());
        assertEquals(new BigDecimal("3.00"), result.getPointsDeductAmount());
        assertEquals(new BigDecimal("34.00"), result.getPayableAmount());
        assertEquals(2, result.getDiscountSnapshots().size());
        assertTrue(result.getPointsPlan().getNeedHold());
        assertEquals(300, result.getPointsPlan().getHoldPoints());
        assertEquals("PRE_HOLD", result.getPointsPlan().getStatus());
    }

    @Test
    void calculateShouldSkipCouponWhenThresholdNotReached() {
        OrderPricingServiceImpl service = new OrderPricingServiceImpl();

        OrderPricingRequestDTO request = new OrderPricingRequestDTO();
        request.setTenantId(9L);
        request.setPlatformUserId(100L);
        request.setOrderNo("SO1002");
        request.setItems(List.of(item(1L, "饮品", "20.00", 1)));
        request.setSelectedCoupon(coupon(301L, 201L, CouponTypeEnum.FULL_REDUCTION.name(), "30.00", "8.00", null));

        OrderPricingResultVO result = service.calculate(request);

        assertEquals(BigDecimal.ZERO.setScale(2), result.getCouponDiscountAmount());
        assertEquals(new BigDecimal("20.00"), result.getPayableAmount());
        assertTrue(result.getDiscountSnapshots().isEmpty());
        assertFalse(result.getPointsPlan().getNeedHold());
    }

    @Test
    void calculateShouldUseCouponEligibleAmountForThreshold() {
        OrderPricingServiceImpl service = new OrderPricingServiceImpl();

        CouponDiscountCandidateDTO coupon = coupon(301L, 201L, CouponTypeEnum.FULL_REDUCTION.name(), "30.00", "8.00", null);
        coupon.setEligibleAmount(new BigDecimal("20.00"));

        OrderPricingRequestDTO request = new OrderPricingRequestDTO();
        request.setTenantId(9L);
        request.setPlatformUserId(100L);
        request.setOrderNo("SO1003");
        request.setItems(List.of(
                item(1L, "饮品", "20.00", 1),
                item(2L, "甜点", "80.00", 1)
        ));
        request.setSelectedCoupon(coupon);

        OrderPricingResultVO result = service.calculate(request);

        assertEquals(BigDecimal.ZERO.setScale(2), result.getCouponDiscountAmount());
        assertEquals(new BigDecimal("100.00"), result.getPayableAmount());
        assertTrue(result.getDiscountSnapshots().isEmpty());
    }

    private OrderPricingItemDTO item(Long productId, String category, String price, int quantity) {
        OrderPricingItemDTO item = new OrderPricingItemDTO();
        item.setProductId(productId);
        item.setCategory(category);
        item.setUnitPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }

    private PromotionDiscountCandidateDTO activity(Long activityId, Long ruleId, String discountAmount) {
        PromotionDiscountCandidateDTO activity = new PromotionDiscountCandidateDTO();
        activity.setActivityId(activityId);
        activity.setActivityRuleId(ruleId);
        activity.setDiscountType("FULL_REDUCTION");
        activity.setDiscountAmount(new BigDecimal(discountAmount));
        activity.setRuleSnapshotJson("{\"thresholdAmount\":\"30.00\"}");
        return activity;
    }

    private CouponDiscountCandidateDTO coupon(Long userCouponId,
                                              Long couponTemplateId,
                                              String couponType,
                                              String thresholdAmount,
                                              String discountAmount,
                                              String discountRate) {
        CouponDiscountCandidateDTO coupon = new CouponDiscountCandidateDTO();
        coupon.setUserCouponId(userCouponId);
        coupon.setCouponTemplateId(couponTemplateId);
        coupon.setCouponType(couponType);
        coupon.setCouponStatus(UserCouponStatusEnum.RECEIVED.name());
        coupon.setCanStackOtherCoupon(Boolean.TRUE);
        coupon.setThresholdAmount(new BigDecimal(thresholdAmount));
        coupon.setDiscountAmount(new BigDecimal(discountAmount));
        if (discountRate != null) {
            coupon.setDiscountRate(new BigDecimal(discountRate));
        }
        coupon.setRuleSnapshotJson("{\"couponType\":\"" + couponType + "\"}");
        return coupon;
    }
}
