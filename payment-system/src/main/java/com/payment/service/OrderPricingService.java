package com.payment.service;

import com.payment.dto.pricing.OrderPricingRequestDTO;
import com.payment.dto.pricing.OrderPricingResultVO;

/**
 * 订单定价服务接口。
 * <p>
 * 负责订单金额的计算，包括商品原价汇总、优惠券抵扣、
 * 积分预占计划、运费计算以及最终应付金额的确定。
 * 在用户下单前调用，为用户提供订单价格预览。
 */
public interface OrderPricingService {

    /**
     * 计算订单优惠、积分预占计划和应付金额。
     *
     * @param request 订单定价请求 DTO，包含商品明细、优惠券信息、积分抵扣设置等
     * @return 订单定价结果视图对象，包含各项金额明细和最终应付金额
     */
    OrderPricingResultVO calculate(OrderPricingRequestDTO request);
}
