package com.payment.controller;

import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.AdminOrderListVO;
import com.payment.dto.AdminPaymentBillVO;
import com.payment.dto.AdminRechargeOrderVO;
import com.payment.dto.AdminTradeOverviewVO;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.service.V1AdminService;
import jakarta.validation.constraints.Min;
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

    @SaCheckPermission(type = "admin", value = {"admin:trade:overview", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/trades/overview")
    public Result<AdminTradeOverviewVO> getTradeOverview() {
        return Result.success(v1AdminService.getTradeOverview());
    }

    @SaCheckPermission(type = "admin", value = {"admin:trade:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/orders")
    public Result<PageResult<AdminOrderListVO>> listOrders(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                            @RequestParam(required = false) String orderNo,
                                                            @RequestParam(required = false) String orderStatus,
                                                            @RequestParam(required = false) String payStatus,
                                                            @RequestParam(required = false) Long tenantId) {
        Page<AdminOrderListVO> page = v1AdminService.listOrders(current, size, orderNo, orderStatus, payStatus, tenantId);
        return Result.success(PageResult.from(page));
    }

    @SaCheckPermission(type = "admin", value = {"admin:trade:detail", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/orders/{orderNo}")
    public Result<SalesOrderDetailVO> getOrderDetail(@PathVariable String orderNo) {
        return Result.success(v1AdminService.getOrderDetail(orderNo));
    }

    @SaCheckPermission(type = "admin", value = {"admin:trade:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/payment-bills")
    public Result<PageResult<AdminPaymentBillVO>> listPaymentBills(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                                    @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                                    @RequestParam(required = false) String bizType,
                                                                    @RequestParam(required = false) String payStatus,
                                                                    @RequestParam(required = false) String channelCode) {
        Page<AdminPaymentBillVO> page = v1AdminService.listPaymentBills(current, size, bizType, payStatus, channelCode);
        return Result.success(PageResult.from(page));
    }

    @SaCheckPermission(type = "admin", value = {"admin:trade:list", "admin:dashboard"}, mode = SaMode.OR)
    @GetMapping("/recharge-orders")
    public Result<PageResult<AdminRechargeOrderVO>> listRechargeOrders(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                                        @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                                        @RequestParam(required = false) String walletType,
                                                                        @RequestParam(required = false) String bizStatus,
                                                                        @RequestParam(required = false) Long tenantId) {
        Page<AdminRechargeOrderVO> page = v1AdminService.listRechargeOrders(current, size, walletType, bizStatus, tenantId);
        return Result.success(PageResult.from(page));
    }
}
