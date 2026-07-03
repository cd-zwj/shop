package com.payment.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单定价请求数据传输对象，用于向定价引擎提交订单的完整定价计算请求。
 */
@Data
public class OrderPricingRequestDTO {
    /** 商户租户 ID */
    private Long tenantId;
    /** 下单用户 ID */
    private Long platformUserId;
    /** 订单编号 */
    private String orderNo;
    /** 订单商品项列表 */
    private List<OrderPricingItemDTO> items = new ArrayList<>();
    /** 活动优惠候选项列表 */
    private List<PromotionDiscountCandidateDTO> promotionCandidates = new ArrayList<>();
    /** 用户选择的优惠券 */
    private CouponDiscountCandidateDTO selectedCoupon;
    /** 用户可用积分总数 */
    private Integer availablePoints;
    /** 用户期望使用的积分数 */
    private Integer requestedPoints;
    /** 每积分抵扣金额（如 1 积分 = 0.01 元） */
    private BigDecimal pointAmount;
}
