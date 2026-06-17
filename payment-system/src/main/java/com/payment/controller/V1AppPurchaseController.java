package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.OrderDeliveryVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 用户端"我的已购"接口。
 *
 * 不区分商品类型，所有已购商品（实物/虚拟/卡密/服务/订阅）统一从 order_delivery_record 查询，
 * 前端按 productType 决定如何渲染 payload。
 */
@RestController
@RequestMapping("/v1/app/purchases")
@RequiredArgsConstructor
public class V1AppPurchaseController {

    private final OrderDeliveryService orderDeliveryService;

    @SaCheckLogin
    @GetMapping
    public Result<PageResult<OrderDeliveryVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Long userId = PlatformSessionHelper.getPlatformUserId();
        Page<OrderDeliveryRecord> page = orderDeliveryService.listUserDeliveries(userId, status, current, size);
        return Result.success(PageResult.from(page, OrderDeliveryVO::from));
    }

    @SaCheckLogin
    @GetMapping("/{id}")
    public Result<OrderDeliveryVO> detail(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        Long userId = PlatformSessionHelper.getPlatformUserId();
        OrderDeliveryRecord record = orderDeliveryService.getUserDelivery(userId, id);
        return Result.success(OrderDeliveryVO.from(record));
    }

    @SaCheckLogin
    @PostMapping("/{id}/confirm")
    public Result<OrderDeliveryVO> confirm(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        Long userId = PlatformSessionHelper.getPlatformUserId();
        OrderDeliveryRecord record = orderDeliveryService.confirmReceived(userId, id);
        return Result.success(OrderDeliveryVO.from(record));
    }
}
