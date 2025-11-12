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
     * 发起支付
     */
    PayResponseDTO pay(Long userId, String orderNo);
    
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

