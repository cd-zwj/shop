package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.AppCreateOrderDTO;
import com.payment.dto.OrderPaymentVO;
import com.payment.entity.SalesOrder;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.service.AppOrderService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.SalesOrderDetailVO;
import com.payment.vo.SalesOrderListVO;
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
    public Result<PageResult<SalesOrderListVO>> listOrders(@RequestParam(defaultValue = "1") Integer current,
                                                      @RequestParam(defaultValue = "10") Integer size) {
        Page<SalesOrder> page = appOrderService.listOrders(PlatformSessionHelper.getPlatformUserId(), current, size);
        return Result.success(PageResult.from(page, SalesOrderListVO::from));
    }

    @SaCheckLogin
    @GetMapping("/{orderNo}")
    public Result<SalesOrderDetailVO> getOrder(@PathVariable String orderNo) {
        com.payment.dto.SalesOrderDetailVO detailVO = appOrderService.getOrderDetail(PlatformSessionHelper.getPlatformUserId(), orderNo);
        return Result.success(SalesOrderDetailVO.from(detailVO));
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
