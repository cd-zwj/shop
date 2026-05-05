package com.payment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.dto.CreateOrderDTO;
import com.payment.dto.PayResponseDTO;
import com.payment.entity.PaymentOrder;

import java.util.Map;

/**
 * 订单服务接口
 */
public interface PaymentOrderService extends IService<PaymentOrder> {
    
    /**
     * 创建订单
     */
    PaymentOrder createOrder(Long userId, CreateOrderDTO dto);
    
    /**
     * 支付订单
     * @param userId 用户ID
     * @param orderNo 订单号
     * @param tradeType 交易类型（可选，默认NATIVE）
     * @return 支付响应
     */
    PayResponseDTO pay(Long userId, String orderNo, String tradeType);
    
    /**
     * 查询订单
     */
    PaymentOrder getOrderByNo(String orderNo);
    
    /**
     * 取消订单
     */
    void cancelOrder(String orderNo);
    
    /**
     * 处理支付回调
     */
    void handlePayNotify(String payType, Map<String, String> params);
    
    /**
     * 查询用户订单列表
     */
    IPage<PaymentOrder> listUserOrders(Long userId, Page<PaymentOrder> page, String orderStatus);
}

