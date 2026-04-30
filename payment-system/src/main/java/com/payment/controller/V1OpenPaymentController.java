package com.payment.controller;

import com.payment.common.Result;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.service.PaymentBillV1Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 开放支付回调入口。
 *
 * 统一入口根据 billNo 和 channelCode 找到支付单与支付渠道，再转异步处理。
 */
@RestController
@RequestMapping("/v1/open/payments")
@RequiredArgsConstructor
public class V1OpenPaymentController {

    private final PaymentBillV1Service paymentBillV1Service;

    @PostMapping("/callbacks/{channelCode}")
    public Result<Void> handleCallback(@PathVariable String channelCode, @RequestBody PaymentCallbackDTO dto) {
        paymentBillV1Service.handleCallback(channelCode, dto);
        return Result.success();
    }

    @PostMapping("/callbacks/{channelCode}/recharge")
    public Result<Void> handleRechargeCallback(@PathVariable String channelCode, @RequestBody PaymentCallbackDTO dto) {
        paymentBillV1Service.handleCallback(channelCode, dto);
        return Result.success();
    }

    @PostMapping("/callbacks/{channelCode}/order")
    public Result<Void> handleOrderCallback(@PathVariable String channelCode, @RequestBody PaymentCallbackDTO dto) {
        paymentBillV1Service.handleCallback(channelCode, dto);
        return Result.success();
    }
}
