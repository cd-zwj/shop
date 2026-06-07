package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.entity.RefundApplication;
import com.payment.service.RefundApplicationService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.RefundApplicationVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * v1 商户端退款审核接口。
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/refunds")
@RequiredArgsConstructor
public class V1MerchantRefundController {

    private final RefundApplicationService refundApplicationService;
    private final V1MerchantSupportService v1MerchantSupportService;

    @SaCheckLogin
    @GetMapping
    public Result<PageResult<RefundApplicationVO>> listTenantRefunds(@PathVariable Long tenantId,
                                                                      @RequestParam(required = false) String status,
                                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);

        Page<RefundApplication> page = refundApplicationService.listTenantRefunds(tenantId, status, pageNum, pageSize);
        return Result.success(PageResult.from(page, RefundApplicationVO::from));
    }

    @SaCheckLogin
    @PutMapping("/{refundId}/audit")
    public Result<Void> auditRefund(@PathVariable Long tenantId,
                                     @PathVariable Long refundId,
                                     @RequestBody AuditRefundRequest request) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);

        refundApplicationService.auditRefund(tenantId, refundId, platformUserId,
                request.isApproved(), request.getRejectReason());
        return Result.success();
    }

    @Data
    public static class AuditRefundRequest {
        private boolean approved;
        private String rejectReason;
    }
}
