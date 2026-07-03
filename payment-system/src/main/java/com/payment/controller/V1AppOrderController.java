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
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端用户订单控制器。
 * <p>
 * 提供用户创建订单、查询订单列表、查看订单详情、重新支付、取消订单等接口。
 * 所有接口均需要platform用户登录态。
 * <p>
 * 路径前缀：/v1/app/orders，需要platform用户登录。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app/orders")
@RequiredArgsConstructor
public class V1AppOrderController {

    private final AppOrderService appOrderService;

    /**
     * 创建订单。
     * <p>
     * 用户选定商品和数量后提交订单，系统计算优惠券折扣、积分抵扣等，
     * 生成订单并返回支付信息（支付链接或二维码）。
     *
     * @param dto 创建订单请求DTO，包含商品ID、数量、优惠券ID等
     * @return 订单支付信息，包含支付链接和订单号
     */
    @SaCheckLogin(type = "platform")
    @PostMapping
    public Result<OrderPaymentVO> createOrder(@Valid @RequestBody AppCreateOrderDTO dto) {
        return Result.success(appOrderService.createOrder(PlatformSessionHelper.getPlatformUserId(), dto));
    }

    /**
     * 查询当前用户的订单列表。
     * <p>
     * 分页查询当前登录用户的所有订单，按创建时间倒序排列。
     *
     * @param current 页码，默认1，必须大于0
     * @param size    每页条数，默认10，必须大于0
     * @return 订单列表分页结果
     */
    @SaCheckLogin(type = "platform")
    @GetMapping
    public Result<PageResult<SalesOrderListVO>> listOrders(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                      @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Page<SalesOrder> page = appOrderService.listOrders(PlatformSessionHelper.getPlatformUserId(), current, size);
        return Result.success(PageResult.from(page, SalesOrderListVO::from));
    }

    /**
     * 查询订单详情。
     * <p>
     * 根据订单号获取订单的完整详情，包括商品信息、支付状态、物流信息等。
     * 仅能查看当前登录用户自己的订单。
     *
     * @param orderNo 订单编号
     * @return 订单详情信息
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/{orderNo}")
    public Result<SalesOrderDetailVO> getOrder(@PathVariable String orderNo) {
        com.payment.dto.SalesOrderDetailVO detailVO = appOrderService.getOrderDetail(PlatformSessionHelper.getPlatformUserId(), orderNo);
        return Result.success(SalesOrderDetailVO.from(detailVO));
    }

    /**
     * 重新支付订单。
     * <p>
     * 对未支付或支付失败的订单发起重新支付，可选择不同的支付渠道。
     * 默认使用支付宝PC支付。
     *
     * @param orderNo            订单编号
     * @param paymentChannelCode 支付渠道编码，默认ALIPAY_PAGE
     * @return 新的支付信息（支付链接等）
     */
    @SaCheckLogin(type = "platform")
    @PostMapping("/{orderNo}/repay")
    public Result<OrderPaymentVO> repayOrder(@PathVariable String orderNo,
                                             @RequestParam(defaultValue = "ALIPAY_PAGE") PaymentChannelCodeEnum paymentChannelCode) {
        return Result.success(appOrderService.repayOrder(PlatformSessionHelper.getPlatformUserId(), orderNo, paymentChannelCode));
    }

    /**
     * 取消订单。
     * <p>
     * 用户主动取消未支付的订单，已支付的订单不可通过此接口取消（需走退款流程）。
     *
     * @param orderNo 订单编号
     * @return 取消结果
     */
    @SaCheckLogin(type = "platform")
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        appOrderService.cancelOrder(PlatformSessionHelper.getPlatformUserId(), orderNo);
        return Result.success();
    }
}
