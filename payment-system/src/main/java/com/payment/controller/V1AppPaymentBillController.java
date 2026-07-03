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
 * C端用户支付单控制器。
 * <p>
 * 提供支付单查询、支付状态同步、支付状态查询等接口。
 * 支持用户查看自己的支付单详情，以及主动同步支付单状态（用于支付回调延迟场景）。
 * 包含支付单归属校验，防止水平越权访问。
 * <p>
 * 路径前缀：/v1/app/payment-bills，需要platform用户登录。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app/payment-bills")
@RequiredArgsConstructor
public class V1AppPaymentBillController {

    private final PaymentBillV1Service paymentBillV1Service;

    /**
     * 查询支付单详情。
     * <p>
     * 根据支付单号获取支付单的完整信息，包括金额、支付状态、支付渠道等。
     * 仅能查询当前登录用户自己的支付单。
     *
     * @param billNo 支付单号
     * @return 支付单详情信息
     * @throws BusinessException 支付单不存在或无权访问时抛出异常
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/{billNo}")
    public Result<PaymentBillVO> getPaymentBill(@PathVariable String billNo) {
        PaymentBill bill = paymentBillV1Service.getByBillNo(billNo);
        if (bill == null) {
            throw new BusinessException("支付单不存在");
        }
        checkOwnership(bill);
        return Result.success(PaymentBillVO.from(bill));
    }

    /**
     * 同步支付单状态。
     * <p>
     * 主动向支付渠道查询最新支付状态并更新本地记录，
     * 适用于支付回调延迟或用户支付后页面未及时刷新的场景。
     *
     * @param billNo 支付单号
     * @return 同步后的支付单信息
     * @throws BusinessException 支付单不存在或无权访问时抛出异常
     */
    @SaCheckLogin(type = "platform")
    @PostMapping("/{billNo}/sync")
    public Result<PaymentBillVO> syncPaymentBill(@PathVariable String billNo) {
        PaymentBill bill = paymentBillV1Service.getByBillNo(billNo);
        if (bill == null) {
            throw new BusinessException("支付单不存在");
        }
        checkOwnership(bill);
        return Result.success(PaymentBillVO.from(paymentBillV1Service.syncBillStatus(billNo)));
    }

    /**
     * 查询支付单状态。
     * <p>
     * 轻量级接口，仅返回支付单号和支付状态，适用于前端轮询支付结果。
     *
     * @param billNo 支付单号，不能为空
     * @return 支付单状态信息（仅包含单号和状态）
     * @throws BusinessException 支付单不存在或无权访问时抛出异常
     */
    @SaCheckLogin(type = "platform")
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
