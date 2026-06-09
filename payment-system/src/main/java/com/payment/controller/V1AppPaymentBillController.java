package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.BusinessException;
import com.payment.common.Result;
import com.payment.dto.BillStatusVO;
import com.payment.entity.PaymentBill;
import com.payment.service.PaymentBillV1Service;
import com.payment.util.UserContext;
import com.payment.vo.PaymentBillVO;
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
    public Result<PaymentBillVO> getPaymentBill(@PathVariable String billNo) {
        PaymentBill bill = paymentBillV1Service.getByBillNo(billNo);
        if (bill == null) {
            throw new BusinessException("支付单不存在");
        }
        checkOwnership(bill);
        return Result.success(PaymentBillVO.from(bill));
    }

    @SaCheckLogin
    @PostMapping("/{billNo}/sync")
    public Result<PaymentBillVO> syncPaymentBill(@PathVariable String billNo) {
        PaymentBill bill = paymentBillV1Service.getByBillNo(billNo);
        if (bill == null) {
            throw new BusinessException("支付单不存在");
        }
        checkOwnership(bill);
        return Result.success(PaymentBillVO.from(paymentBillV1Service.syncBillStatus(billNo)));
    }

    @SaCheckLogin
    @GetMapping("/{billNo}/status")
    public Result<BillStatusVO> getBillStatus(@PathVariable String billNo) {
        if (billNo == null || billNo.isBlank()) {
            throw new BusinessException("账单号不能为空");
        }
        PaymentBill bill = paymentBillV1Service.getByBillNo(billNo);
        if (bill == null) {
            throw new BusinessException("支付单不存在");
        }
        checkOwnership(bill);
        BillStatusVO vo = new BillStatusVO();
        vo.setBillNo(bill.getBillNo());
        vo.setPayStatus(bill.getPayStatus());
        return Result.success(vo);
    }

    /** 校验支付单归属，防止水平越权 */
    private void checkOwnership(PaymentBill bill) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (bill.getPlatformUserId() != null && !bill.getPlatformUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权访问该支付单");
        }
    }
}
