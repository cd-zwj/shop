package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.RefundApplication;
import com.payment.service.RefundApplicationService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.RefundApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 用户端退款申请接口。
 */
@RestController
@RequestMapping("/v1/app/tenants/{tenantId}/refunds")
@RequiredArgsConstructor
public class V1AppRefundController {

    private final RefundApplicationService refundApplicationService;

    @SaCheckLogin
    @PostMapping
    public Result<RefundApplicationVO> createRefund(@PathVariable Long tenantId,
                                                     @Valid @RequestBody RefundCreateDTO dto) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        RefundApplication app = refundApplicationService.createRefund(platformUserId, tenantId, dto);
        return Result.success(RefundApplicationVO.from(app));
    }

    @SaCheckLogin
    @GetMapping
    public Result<PageResult<RefundApplicationVO>> listMyRefunds(@PathVariable Long tenantId,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        Page<RefundApplication> page = refundApplicationService.listMyRefunds(platformUserId, tenantId, status, pageNum, pageSize);
        return Result.success(PageResult.from(page, RefundApplicationVO::from));
    }

    @SaCheckLogin
    @GetMapping("/{refundId}")
    public Result<RefundApplicationVO> getRefundDetail(@PathVariable Long tenantId,
                                                        @PathVariable Long refundId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        RefundApplication app = refundApplicationService.getRefundDetail(platformUserId, tenantId, refundId);
        return Result.success(RefundApplicationVO.from(app));
    }

    @SaCheckLogin
    @PutMapping("/{refundId}/cancel")
    public Result<Void> cancelRefund(@PathVariable Long tenantId,
                                      @PathVariable Long refundId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        refundApplicationService.cancelRefund(platformUserId, tenantId, refundId);
        return Result.success();
    }
}
