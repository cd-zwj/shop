package com.payment.controller;

import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.Result;
import com.payment.dto.AdminOrderListVO;
import com.payment.dto.AdminPaymentBillVO;
import com.payment.dto.AdminRechargeOrderVO;
import com.payment.dto.AdminTradeOverviewVO;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.service.V1AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 管理端订单/支付/充值总览接口。
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class V1AdminTradeController {

    private final V1AdminService v1AdminService;

    @SaCheckPermission(value = {"admin:trade:overview", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/trades/overview")
    public Result<AdminTradeOverviewVO> getTradeOverview() {
        return Result.success(v1AdminService.getTradeOverview());
    }

    @SaCheckPermission(value = {"admin:trade:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/orders")
    public Result<Page<AdminOrderListVO>> listOrders(@RequestParam(defaultValue = "1") Integer current,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @RequestParam(required = false) String orderNo,
                                                     @RequestParam(required = false) String orderStatus,
                                                     @RequestParam(required = false) String payStatus,
                                                     @RequestParam(required = false) Long tenantId) {
        return Result.success(v1AdminService.listOrders(current, size, orderNo, orderStatus, payStatus, tenantId));
    }

    @SaCheckPermission(value = {"admin:trade:detail", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/orders/{orderNo}")
    public Result<SalesOrderDetailVO> getOrderDetail(@PathVariable String orderNo) {
        return Result.success(v1AdminService.getOrderDetail(orderNo));
    }

    @SaCheckPermission(value = {"admin:trade:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/payment-bills")
    public Result<Page<AdminPaymentBillVO>> listPaymentBills(@RequestParam(defaultValue = "1") Integer current,
                                                             @RequestParam(defaultValue = "10") Integer size,
                                                             @RequestParam(required = false) String bizType,
                                                             @RequestParam(required = false) String payStatus,
                                                             @RequestParam(required = false) String channelCode) {
        return Result.success(v1AdminService.listPaymentBills(current, size, bizType, payStatus, channelCode));
    }

    @SaCheckPermission(value = {"admin:trade:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/recharge-orders")
    public Result<Page<AdminRechargeOrderVO>> listRechargeOrders(@RequestParam(defaultValue = "1") Integer current,
                                                                 @RequestParam(defaultValue = "10") Integer size,
                                                                 @RequestParam(required = false) String walletType,
                                                                 @RequestParam(required = false) String bizStatus,
                                                                 @RequestParam(required = false) Long tenantId) {
        return Result.success(v1AdminService.listRechargeOrders(current, size, walletType, bizStatus, tenantId));
    }
}
