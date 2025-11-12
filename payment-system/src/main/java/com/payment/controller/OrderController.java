package com.payment.controller;

import com.payment.annotation.RequireAuth;
import com.payment.common.Result;
import com.payment.dto.CreateOrderDTO;
import com.payment.dto.PayResponseDTO;
import com.payment.entity.PaymentOrder;
import com.payment.service.PaymentOrderService;

import com.payment.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 订单控制器
 */
 
 
@RestController
@RequestMapping("/order")
@RequireAuth  // 整个Controller都需要认证
public class OrderController {
    
    @Autowired
    private PaymentOrderService paymentOrderService;

    @PostMapping("/create")
    public Result<PaymentOrder> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        PaymentOrder order = paymentOrderService.createOrder(userId, dto);
        return Result.success(order);
    }

    @PostMapping("/pay")
    public Result<PayResponseDTO> pay(@RequestParam String orderNo) {
        Long userId = UserContext.getCurrentUserId();
        PayResponseDTO payResponse = paymentOrderService.pay(userId, orderNo);
        return Result.success(payResponse);
    }

    @GetMapping("/query")
    public Result<PaymentOrder> queryOrder(@RequestParam String orderNo) {
        PaymentOrder order = paymentOrderService.getOrderByNo(orderNo);
        return Result.success(order);
    }

    @PostMapping("/cancel")
    public Result<Void> cancelOrder(@RequestParam String orderNo) {
        paymentOrderService.cancelOrder(orderNo);
        return Result.success();
    }
}

