package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
import com.payment.dto.MerchantWorkbenchTaskVO;
import com.payment.dto.MerchantWorkbenchTodoSummaryVO;
import com.payment.service.V1MerchantWorkbenchService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户工作台聚合接口。
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/workbench")
@RequiredArgsConstructor
public class V1MerchantWorkbenchController {

    private final V1MerchantWorkbenchService workbenchService;
    private final V1MerchantSupportService v1MerchantSupportService;

    @SaCheckLogin(type = "merchant")
    @GetMapping("/todos")
    public Result<MerchantWorkbenchTodoSummaryVO> getTodoSummary(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.DASHBOARD_VIEW);
        return Result.success(workbenchService.getTodoSummary(tenantId));
    }

    @SaCheckLogin(type = "merchant")
    @GetMapping("/tasks")
    public Result<PageResult<MerchantWorkbenchTaskVO>> listVisibleTasks(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(defaultValue = "compensation") String type,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数必须大于0") Integer pageSize) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.DASHBOARD_VIEW);
        return Result.success(workbenchService.listVisibleTasks(tenantId, type, pageNum, pageSize));
    }
}
