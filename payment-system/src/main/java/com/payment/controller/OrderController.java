package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
@SaCheckLogin
public class OrderController {
    
    @Autowired
    private PaymentOrderService paymentOrderService;

    @SaCheckPermission("order:create")
    @PostMapping("/create")
    public Result<PaymentOrder> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        PaymentOrder order = paymentOrderService.createOrder(userId, dto);
        return Result.success(order);
    }

    @SaCheckPermission("order:pay")
    @PostMapping("/pay")
    public Result<PayResponseDTO> pay(@RequestParam String orderNo) {
        Long userId = UserContext.getCurrentUserId();
        PayResponseDTO payResponse = paymentOrderService.pay(userId, orderNo, "NATIVE");
        return Result.success(payResponse);
    }

    @SaCheckPermission("order:query")
    @GetMapping("/query")
    public Result<PaymentOrder> queryOrder(@RequestParam String orderNo) {
        Long userId = UserContext.getCurrentUserId();
        PaymentOrder order = paymentOrderService.getOrderByNo(orderNo);
        if (order == null) {
            throw new com.payment.common.BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new com.payment.common.BusinessException(403, "无权访问该订单");
        }
        return Result.success(order);
    }

    @SaCheckPermission("order:cancel")
    @PostMapping("/cancel")
    public Result<Void> cancelOrder(@RequestParam String orderNo) {
        Long userId = UserContext.getCurrentUserId();
        PaymentOrder order = paymentOrderService.getOrderByNo(orderNo);
        if (order == null) {
            throw new com.payment.common.BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new com.payment.common.BusinessException(403, "无权操作该订单");
        }
        paymentOrderService.cancelOrder(orderNo);
        return Result.success();
    }
}

