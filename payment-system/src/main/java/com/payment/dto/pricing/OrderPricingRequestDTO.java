package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单定价请求。
 */
@Data
public class OrderPricingRequestDTO {
    private Long tenantId;
    private Long platformUserId;
    private String orderNo;
    private List<OrderPricingItemDTO> items = new ArrayList<>();
    private List<PromotionDiscountCandidateDTO> promotionCandidates = new ArrayList<>();
    private CouponDiscountCandidateDTO selectedCoupon;
    private Integer availablePoints;
    private Integer requestedPoints;
    private BigDecimal pointAmount;
}
