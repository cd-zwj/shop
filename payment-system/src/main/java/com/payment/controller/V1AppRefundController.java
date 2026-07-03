package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.annotation.RateLimit;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.RefundApplication;
import com.payment.service.RefundApplicationService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.RefundApplicationVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端用户退款申请控制器。
 * <p>
 * 提供退款申请的创建、查询列表、查看详情、取消退款等接口。
 * 退款申请按商户隔离，用户只能操作自己在指定商户下的退款申请。
 * 创建退款请求配置了限流策略，防止恶意刷退款。
 * <p>
 * 路径前缀：/v1/app/tenants/{tenantId}/refunds，需要platform用户登录。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app/tenants/{tenantId}/refunds")
@RequiredArgsConstructor
public class V1AppRefundController {

    private final RefundApplicationService refundApplicationService;

    /**
     * 创建退款申请。
     * <p>
     * 用户对已支付的订单发起退款申请，需指定退款原因和退款金额。
     * 同一商户每5分钟最多发起5次退款请求。
     *
     * @param tenantId 商户ID，必须大于0
     * @param dto      退款申请信息（订单号、退款原因、退款金额等）
     * @return 创建的退款申请信息
     */
    @RateLimit(prefix = "app:refund:create", key = "#tenantId", window = 300, maxRequests = 5, includeIp = true, message = "退款申请过于频繁，请稍后再试")
    @SaCheckLogin(type = "platform")
    @PostMapping
    public Result<RefundApplicationVO> createRefund(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                     @Valid @RequestBody RefundCreateDTO dto) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        RefundApplication app = refundApplicationService.createRefund(platformUserId, tenantId, dto);
        return Result.success(RefundApplicationVO.from(app));
    }

    /**
     * 查询退款申请列表。
     * <p>
     * 分页查询当前用户在指定商户下的退款申请列表，可按退款状态筛选。
     *
     * @param tenantId 商户ID，必须大于0
     * @param status   退款状态筛选（可选），如pending/approved/rejected
     * @param pageNum  页码，默认1，必须大于0
     * @param pageSize 每页条数，默认10，必须大于0
     * @return 退款申请列表分页结果
     */
    @SaCheckLogin(type = "platform")
    @GetMapping
    public Result<PageResult<RefundApplicationVO>> listMyRefunds(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer pageNum,
                                                                  @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer pageSize) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        Page<RefundApplication> page = refundApplicationService.listMyRefunds(platformUserId, tenantId, status, pageNum, pageSize);
        return Result.success(PageResult.from(page, RefundApplicationVO::from));
    }

    /**
     * 查询退款申请详情。
     * <p>
     * 获取指定退款申请的完整信息，包括退款原因、退款金额、审核状态、处理进度等。
     * 仅能查看当前用户自己的退款申请。
     *
     * @param tenantId 商户ID，必须大于0
     * @param refundId 退款申请ID，必须大于0
     * @return 退款申请详情
     */
    @SaCheckLogin(type = "platform")
    @GetMapping("/{refundId}")
    public Result<RefundApplicationVO> getRefundDetail(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                        @PathVariable @Min(value = 1, message = "ID必须大于0") Long refundId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        RefundApplication app = refundApplicationService.getRefundDetail(platformUserId, tenantId, refundId);
        return Result.success(RefundApplicationVO.from(app));
    }

    /**
     * 取消退款申请。
     * <p>
     * 用户主动取消未审核或审核中的退款申请，已处理完成的退款无法取消。
     *
     * @param tenantId 商户ID，必须大于0
     * @param refundId 退款申请ID，必须大于0
     * @return 取消结果
     */
    @SaCheckLogin(type = "platform")
    @PutMapping("/{refundId}/cancel")
    public Result<Void> cancelRefund(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                      @PathVariable @Min(value = 1, message = "ID必须大于0") Long refundId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        refundApplicationService.cancelRefund(platformUserId, tenantId, refundId);
        return Result.success();
    }
}
