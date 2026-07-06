package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
import com.payment.entity.RefundApplication;
import com.payment.service.RefundApplicationService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.RefundApplicationVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商户端退款审核控制器（Merchant 端）。
 * <p>提供商户对退款申请的列表查询和审核（通过/拒绝）操作。
 * 需要商户角色登录，并通过 RBAC 权限（merchant:refund:list / audit）控制访问。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/refunds")
@RequiredArgsConstructor
public class V1MerchantRefundController {

    private final RefundApplicationService refundApplicationService;
    private final V1MerchantSupportService v1MerchantSupportService;

    /**
     * 分页查询租户下的退款申请列表。
     *
     * @param tenantId 租户 ID
     * @param status   退款状态筛选（可选）
     * @param pageNum  当前页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @return 退款申请分页列表
     */
    @SaCheckPermission(type = "merchant", value = "merchant:refund:list")
    @GetMapping
    public Result<PageResult<RefundApplicationVO>> listTenantRefunds(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                                                      @RequestParam(required = false) String status,
                                                                      @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer pageNum,
                                                                      @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer pageSize) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.REFUND_MANAGE);

        Page<RefundApplication> page = refundApplicationService.listTenantRefunds(tenantId, status, pageNum, pageSize);
        return Result.success(PageResult.from(page, RefundApplicationVO::from));
    }

    /**
     * 审核退款申请（通过或拒绝）。
     *
     * @param tenantId 租户 ID
     * @param refundId 退款申请 ID
     * @param request  审核请求（是否通过、拒绝原因）
     * @return 操作结果
     */
    @SaCheckPermission(type = "merchant", value = "merchant:refund:audit")
    @PutMapping("/{refundId}/audit")
    public Result<Void> auditRefund(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                     @PathVariable @Min(value = 1, message = "ID必须大于0") Long refundId,
                                     @Valid @RequestBody AuditRefundRequest request) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.REFUND_MANAGE);

        refundApplicationService.auditRefund(tenantId, refundId, platformUserId,
                request.isApproved(), request.getRejectReason());
        return Result.success();
    }

    @Data
    public static class AuditRefundRequest {
        private boolean approved;
        @Size(max = 500, message = "拒绝原因不能超过500字")
        private String rejectReason;
    }
}
