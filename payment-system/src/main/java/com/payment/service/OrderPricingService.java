package com.payment.service;

import com.payment.dto.pricing.OrderPricingRequestDTO;
import com.payment.dto.pricing.OrderPricingResultVO;

/**
 * 订单定价服务接口。
 */
public interface OrderPricingService {
    /**
     * 计算订单优惠、积分预占计划和应付金额。
     */
    OrderPricingResultVO calculate(OrderPricingRequestDTO request);
}
