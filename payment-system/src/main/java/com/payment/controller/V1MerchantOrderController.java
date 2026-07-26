package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.annotation.RateLimit;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.OrderFulfillmentAction;
import com.payment.service.AppOrderService;
import com.payment.service.OrderFulfillmentService;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.PickupVerificationVO;
import com.payment.vo.SalesOrderListVO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商户端订单管理控制器（Merchant 端）。
 * <p>显式携带 tenantId，避免一个商户员工绑定多个商户时出现歧义。
 * 提供到店自提订单的列表、详情和取货码核销功能。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/orders")
@RequiredArgsConstructor
public class V1MerchantOrderController {

    private final AppOrderService appOrderService;
    private final V1MerchantSupportService v1MerchantSupportService;
    private final OrderDeliveryService orderDeliveryService;
    private final OrderFulfillmentService orderFulfillmentService;

    /**
     * 分页查询商户订单列表。
     *
     * @param tenantId    租户 ID
     * @param current     当前页码，默认 1
     * @param size        每页条数，默认 10
     * @param orderStatus 订单状态筛选（可选）
     * @param payStatus   支付状态筛选（可选）
     * @param keyword     搜索关键字（可选）
     * @param fulfillmentStatus 履约状态分组筛选（可选）
     * @param deliveryStatus 单个或逗号分隔交付状态筛选（可选）
     * @return 订单分页列表
     */
    @SaCheckLogin(type = "merchant")
    @GetMapping
    public Result<PageResult<SalesOrderListVO>> listOrders(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                      @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                      @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                      @RequestParam(required = false) String orderStatus,
                                                      @RequestParam(required = false) String payStatus,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String fulfillmentStatus,
                                                      @RequestParam(required = false) String deliveryStatus) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.ORDER_MANAGE);

        Page<SalesOrderListVO> result = appOrderService.listMerchantOrderViews(
                tenantId, current, size, orderStatus, payStatus, keyword, fulfillmentStatus, deliveryStatus);
        return Result.success(PageResult.from(result));
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
     * 到店自提核销：仅能核销当前门店的自提订单。
     * 连续错误输入按租户+IP 限流，失败尝试写入审计日志。
     */
    @SaCheckLogin(type = "merchant")
    @RateLimit(prefix = "merchant:pickup:verify", key = "#tenantId", window = 60, maxRequests = 30,
            includeIp = true, message = "核销尝试过于频繁，请稍后再试")
    @PostMapping("/pickups/verify")
    public Result<PickupVerificationVO> verifyPickup(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                @RequestBody VerifyPickupRequest request) {
        Long operatorId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, operatorId, MerchantPermission.ORDER_MANAGE);
        OrderDeliveryRecord record = orderDeliveryService.verifyPickup(tenantId, request.getStoreId(), request.getPickupCode(), operatorId);
        return Result.success(PickupVerificationVO.from(record, request.getStoreId()));
    }

    @SaCheckLogin(type = "merchant")
    @PostMapping("/{orderNo}/fulfillment/start")
    public Result<Void> startPreparation(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                         @PathVariable String orderNo,
                                         @RequestBody(required = false) FulfillmentRequest request) {
        Long operatorId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, operatorId, MerchantPermission.ORDER_MANAGE);
        orderFulfillmentService.startPreparation(tenantId, orderNo, operatorId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @SaCheckLogin(type = "merchant")
    @PostMapping("/{orderNo}/fulfillment/complete")
    public Result<Void> completePreparation(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                            @PathVariable String orderNo,
                                            @RequestBody(required = false) FulfillmentRequest request) {
        Long operatorId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, operatorId, MerchantPermission.ORDER_MANAGE);
        orderFulfillmentService.completePreparation(tenantId, orderNo, operatorId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @SaCheckLogin(type = "merchant")
    @GetMapping("/{orderNo}/fulfillment-actions")
    public Result<java.util.List<OrderFulfillmentAction>> listFulfillmentActions(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable String orderNo) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.ORDER_MANAGE);
        return Result.success(orderFulfillmentService.listActions(tenantId, orderNo));
    }

    @Data
    public static class VerifyPickupRequest {
        private Long storeId;
        private String pickupCode;
    }

    @Data
    public static class FulfillmentRequest {
        @Size(max = 255, message = "履约备注不能超过255个字符")
        private String remark;
    }
}
