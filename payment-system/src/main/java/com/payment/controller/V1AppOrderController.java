package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.Result;
import com.payment.dto.AppCreateOrderDTO;
import com.payment.dto.OrderPaymentVO;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.entity.SalesOrder;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.service.AppOrderService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 用户端订单接口。
 */
@RestController
@RequestMapping("/v1/app/orders")
@RequiredArgsConstructor
public class V1AppOrderController {

    private final AppOrderService appOrderService;

    @SaCheckLogin
    @PostMapping
    public Result<OrderPaymentVO> createOrder(@Valid @RequestBody AppCreateOrderDTO dto) {
        return Result.success(appOrderService.createOrder(PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckLogin
    @GetMapping
    public Result<Page<SalesOrder>> listOrders(@RequestParam(defaultValue = "1") Integer current,
                                               @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(appOrderService.listOrders(PlatformSessionHelper.getPlatformUserId(), current, size));
    }

    @SaCheckLogin
    @GetMapping("/{orderNo}")
    public Result<SalesOrderDetailVO> getOrder(@PathVariable String orderNo) {
        return Result.success(appOrderService.getOrderDetail(PlatformSessionHelper.getPlatformUserId(), orderNo));
    }

    @SaCheckLogin
    @PostMapping("/{orderNo}/repay")
    public Result<OrderPaymentVO> repayOrder(@PathVariable String orderNo,
                                             @RequestParam(defaultValue = "ALIPAY_PAGE") PaymentChannelCodeEnum paymentChannelCode) {
        return Result.success(appOrderService.repayOrder(PlatformSessionHelper.getPlatformUserId(), orderNo, paymentChannelCode));
    }

    @SaCheckLogin
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        appOrderService.cancelOrder(PlatformSessionHelper.getPlatformUserId(), orderNo);
        return Result.success();
    }
}
