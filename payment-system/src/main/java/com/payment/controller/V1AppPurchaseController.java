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
 * C端用户已购商品控制器。
 * <p>
 * 提供已购商品列表查询、详情查看、确认收货等接口。
 * 不区分商品类型，所有已购商品（实物/虚拟/卡密/服务/订阅）统一从 order_delivery_record 查询，
 * 前端按 productType 决定如何渲染 payload。
 * <p>
 * 路径前缀：/v1/app/purchases，需要platform用户登录。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app/purchases")
@RequiredArgsConstructor
public class V1AppPurchaseController {

    private final OrderDeliveryService orderDeliveryService;

    /**
     * 查询已购商品列表。
     * <p>
     * 分页查询当前用户的已购商品/交付记录，可按状态筛选。
     * 包含实物商品、虚拟商品、卡密、服务、订阅等所有类型的已购记录。
     *
     * @param status  交付状态筛选（可选），如pending/delivered/confirmed
     * @param orderNo 订单号筛选（可选），用于从订单详情直达交付记录
     * @param current 页码，默认1，必须大于0
     * @param size    每页条数，默认10，必须大于0
     * @return 已购商品列表分页结果
     */
    @SaCheckLogin(type = "platform")
    @GetMapping
    public Result<PageResult<OrderDeliveryVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Long userId = PlatformSessionHelper.getPlatformUserId();
        Page<OrderDeliveryRecord> page = orderDeliveryService.listUserDeliveries(userId, status, orderNo, current, size);
        return Result.success(PageResult.from(page, OrderDeliveryVO::from));
    }

    /**
     * 查询已购商品详情。
     * <p>
     * 获取指定交付记录的完整信息，包括商品名称、交付内容、交付状态等。
     * 仅能查看当前用户自己的交付记录。
     *
     * @param id 交付记录ID，必须大于0
     * @return 已购商品详情
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/{id}")
    public Result<OrderDeliveryVO> detail(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        Long userId = PlatformSessionHelper.getPlatformUserId();
        OrderDeliveryRecord record = orderDeliveryService.getUserDelivery(userId, id);
        return Result.success(OrderDeliveryVO.from(record));
    }

    /**
     * 确认收货。
     * <p>
     * 用户确认已收到商品或服务，将交付状态标记为已确认。
     * 主要用于实物商品的收货确认，虚拟商品可能自动确认。
     *
     * @param id 交付记录ID，必须大于0
     * @return 更新后的交付记录信息
     */
    @SaCheckLogin(type = "platform")
    @PostMapping("/{id}/confirm")
    public Result<OrderDeliveryVO> confirm(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        Long userId = PlatformSessionHelper.getPlatformUserId();
        OrderDeliveryRecord record = orderDeliveryService.confirmReceived(userId, id);
        return Result.success(OrderDeliveryVO.from(record));
    }
}
