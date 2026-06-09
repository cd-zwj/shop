package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.entity.SalesOrder;
import com.payment.service.AppOrderService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.SalesOrderListVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 商户端订单明细接口。
 *
 * 显式携带 tenantId，避免一个商户员工绑定多个商户时出现歧义。
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/orders")
@RequiredArgsConstructor
public class V1MerchantOrderController {

    private final AppOrderService appOrderService;
    private final V1MerchantSupportService v1MerchantSupportService;

    @SaCheckLogin
    @GetMapping
    public Result<PageResult<SalesOrderListVO>> listOrders(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                      @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                      @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                      @RequestParam(required = false) String orderStatus,
                                                      @RequestParam(required = false) String payStatus,
                                                      @RequestParam(required = false) String keyword) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);

        Page<SalesOrder> result = appOrderService.listMerchantOrders(tenantId, current, size, orderStatus, payStatus, keyword);
        return Result.success(PageResult.from(result, SalesOrderListVO::from));
    }

    @SaCheckLogin
    @GetMapping("/{orderNo}")
    public Result<SalesOrderDetailVO> getOrderDetail(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable String orderNo) {
        return Result.success(appOrderService.getMerchantOrderDetail(tenantId, PlatformSessionHelper.getPlatformUserId(), orderNo));
    }
}
