package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.entity.PaymentBill;
import com.payment.service.PaymentBillV1Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 用户端支付单接口。
 */
@RestController
@RequestMapping("/v1/app/payment-bills")
@RequiredArgsConstructor
public class V1AppPaymentBillController {

    private final PaymentBillV1Service paymentBillV1Service;

    @SaCheckLogin
    @GetMapping("/{billNo}")
    public Result<PaymentBill> getPaymentBill(@PathVariable String billNo) {
        return Result.success(paymentBillV1Service.getByBillNo(billNo));
    }

    @SaCheckLogin
    @PostMapping("/{billNo}/sync")
    public Result<PaymentBill> syncPaymentBill(@PathVariable String billNo) {
        return Result.success(paymentBillV1Service.syncBillStatus(billNo));
    }
}
