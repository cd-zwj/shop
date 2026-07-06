package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.SalesOrder;
import com.payment.service.AppOrderService;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.OrderDeliveryVO;
import com.payment.vo.SalesOrderListVO;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商户端订单管理控制器（Merchant 端）。
 * <p>显式携带 tenantId，避免一个商户员工绑定多个商户时出现歧义。
 * 提供订单列表查询、订单详情查看、实物商品发货和服务商品核销等功能。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/orders")
@RequiredArgsConstructor
public class V1MerchantOrderController {

    private final AppOrderService appOrderService;
    private final V1MerchantSupportService v1MerchantSupportService;
    private final OrderDeliveryService orderDeliveryService;

    /**
     * 分页查询商户订单列表。
     *
     * @param tenantId    租户 ID
     * @param current     当前页码，默认 1
     * @param size        每页条数，默认 10
     * @param orderStatus 订单状态筛选（可选）
     * @param payStatus   支付状态筛选（可选）
     * @param keyword     搜索关键字（可选）
     * @return 订单分页列表
     */
    @SaCheckLogin(type = "merchant")
    @GetMapping
    public Result<PageResult<SalesOrderListVO>> listOrders(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                      @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                      @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                      @RequestParam(required = false) String orderStatus,
                                                      @RequestParam(required = false) String payStatus,
                                                      @RequestParam(required = false) String keyword) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.ORDER_MANAGE);

        Page<SalesOrder> result = appOrderService.listMerchantOrders(tenantId, current, size, orderStatus, payStatus, keyword);
        return Result.success(PageResult.from(result, SalesOrderListVO::from));
    }

    /**
     * 获取订单详情。
     *
     * @param tenantId 租户 ID
     * @param orderNo  订单编号
     * @return 订单详情信息
     */
    @SaCheckLogin(type = "merchant")
    @GetMapping("/{orderNo}")
    public Result<SalesOrderDetailVO> getOrderDetail(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @PathVariable String orderNo) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.ORDER_MANAGE);
        return Result.success(appOrderService.getMerchantOrderDetail(tenantId, platformUserId, orderNo));
    }

    /**
     * 实物商品发货：填写物流单号后将订单项的交付状态置为 DELIVERED。
     */
    @SaCheckLogin(type = "merchant")
    @PostMapping("/items/{orderItemId}/ship")
    public Result<OrderDeliveryVO> shipItem(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                            @PathVariable @Min(value = 1, message = "ID必须大于0") Long orderItemId,
                                            @RequestBody ShipRequest request) {
        v1MerchantSupportService.requirePermission(tenantId, PlatformSessionHelper.getPlatformUserId(), MerchantPermission.ORDER_MANAGE);
        OrderDeliveryRecord record = orderDeliveryService.markShipped(
                tenantId, orderItemId, request.getShippingNo(), request.getLogisticsCompany());
        return Result.success(OrderDeliveryVO.from(record));
    }

    /**
     * 服务商品核销：商户录入用户出示的核销码后将交付状态置为 CONFIRMED。
     */
    @SaCheckLogin(type = "merchant")
    @PostMapping("/services/verify")
    public Result<OrderDeliveryVO> verifyService(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                 @RequestBody VerifyServiceRequest request) {
        v1MerchantSupportService.requirePermission(tenantId, PlatformSessionHelper.getPlatformUserId(), MerchantPermission.ORDER_MANAGE);
        OrderDeliveryRecord record = orderDeliveryService.verifyService(tenantId, request.getVerifyCode());
        return Result.success(OrderDeliveryVO.from(record));
    }

    @Data
    public static class ShipRequest {
        private String shippingNo;
        private String logisticsCompany;
    }

    @Data
    public static class VerifyServiceRequest {
        private String verifyCode;
    }
}
