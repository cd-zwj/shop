package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.BusinessException;
import com.payment.common.Result;
import com.payment.entity.PaymentBill;
import com.payment.service.PaymentBillV1Service;
import com.payment.util.UserContext;
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
        PaymentBill bill = paymentBillV1Service.getByBillNo(billNo);
        if (bill == null) {
            throw new BusinessException("支付单不存在");
        }
        checkOwnership(bill);
        return Result.success(bill);
    }

    @SaCheckLogin
    @PostMapping("/{billNo}/sync")
    public Result<PaymentBill> syncPaymentBill(@PathVariable String billNo) {
        PaymentBill bill = paymentBillV1Service.getByBillNo(billNo);
        if (bill == null) {
            throw new BusinessException("支付单不存在");
        }
        checkOwnership(bill);
        return Result.success(paymentBillV1Service.syncBillStatus(billNo));
    }

    /** 校验支付单归属，防止水平越权 */
    private void checkOwnership(PaymentBill bill) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (bill.getPlatformUserId() != null && !bill.getPlatformUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权访问该支付单");
        }
    }
}
