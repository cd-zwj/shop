package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.service.RefundApplicationService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 平台对待审核售后的介入处理入口。 */
@RestController
@RequestMapping("/v1/admin/tenants/{tenantId}/refunds")
@RequiredArgsConstructor
public class V1AdminAfterSaleController {
    private final RefundApplicationService refundApplicationService;

    @SaCheckPermission(type = "admin", value = "admin:trade:detail")
    @PutMapping("/{refundId}/intervene")
    public Result<Void> intervene(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
                                  @PathVariable @Min(value = 1, message = "ID必须大于0") Long refundId,
                                  @Valid @RequestBody InterventionRequest request) {
        refundApplicationService.intervene(tenantId, refundId, PlatformSessionHelper.getPlatformUserId(),
                request.isApproved(), request.getRemark());
        return Result.success();
    }

    @Data
    public static class InterventionRequest {
        private boolean approved;
        @NotBlank(message = "平台处理说明不能为空")
        @Size(max = 1000, message = "平台处理说明不能超过1000个字符")
        private String remark;
    }
}
