package com.payment.service;

import com.payment.dto.PayResponseDTO;
import com.payment.entity.PaymentOrder;

import java.util.Map;

/**
 * 支付服务接口（微信、支付宝）
 */
public interface PaymentService {
    
    /**
     * 创建支付
     */
    PayResponseDTO createPay(PaymentOrder order);
    
    /**
     * 验证回调签名
     */
    boolean verifyNotify(String payType, Map<String, String> params);
    
    /**
     * 查询订单状态
     */
    Map<String, String> queryOrder(String payType, String orderNo);
}

