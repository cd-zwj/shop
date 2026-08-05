package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.common.PageResult;
import com.payment.entity.RefundApplication;
import com.payment.service.RefundApplicationService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.AdminAfterSaleVO;
import com.payment.vo.AfterSaleActionVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 平台对待审核售后的介入处理入口。 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class V1AdminAfterSaleController {
    private final RefundApplicationService refundApplicationService;

    @SaCheckPermission(type = "admin", value = "admin:after-sale:list")
    @GetMapping("/refunds")
    public Result<PageResult<AdminAfterSaleVO>> listRefunds(
            @RequestParam(required = false) @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Size(max = 64, message = "搜索关键词不能超过64个字符") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数必须大于0")
            @Max(value = 100, message = "每页条数不能超过100") Integer pageSize) {
        Page<RefundApplication> page = refundApplicationService.listAdminRefunds(
                tenantId, status, keyword, pageNum, pageSize);
        return Result.success(PageResult.from(page, AdminAfterSaleVO::from));
    }

    @SaCheckPermission(type = "admin", value = "admin:after-sale:list")
    @GetMapping("/tenants/{tenantId}/refunds/{refundId}")
    public Result<AdminAfterSaleVO> getRefund(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long refundId) {
        return Result.success(AdminAfterSaleVO.from(
                refundApplicationService.getAdminRefund(tenantId, refundId)));
    }

    @SaCheckPermission(type = "admin", value = "admin:after-sale:list")
    @GetMapping("/tenants/{tenantId}/refunds/{refundId}/actions")
    public Result<java.util.List<AfterSaleActionVO>> listActions(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long refundId) {
        return Result.success(refundApplicationService.listActions(tenantId, refundId).stream()
                .map(AfterSaleActionVO::from)
                .toList());
    }

    @SaCheckPermission(type = "admin", value = "admin:after-sale:manage")
    @PutMapping("/tenants/{tenantId}/refunds/{refundId}/intervene")
    public Result<Void> intervene(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                  @PathVariable @Min(value = 1, message = "ID必须大于0") Long refundId,
                                  @Valid @RequestBody InterventionRequest request) {
        refundApplicationService.intervene(tenantId, refundId, PlatformSessionHelper.getPlatformUserId(),
                request.getExpectedStatus(), request.getApproved(), request.getRemark());
        return Result.success();
    }

    @Data
    public static class InterventionRequest {
        @NotNull(message = "平台处理决定不能为空")
        private Boolean approved;
        @NotBlank(message = "售后当前状态不能为空")
        @Pattern(regexp = "PENDING|REJECTED", message = "售后当前状态仅支持PENDING或REJECTED")
        private String expectedStatus;
        @NotBlank(message = "平台处理说明不能为空")
        @Size(max = 1000, message = "平台处理说明不能超过1000个字符")
        private String remark;
    }
}
