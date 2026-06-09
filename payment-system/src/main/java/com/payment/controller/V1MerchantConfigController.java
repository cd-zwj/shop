package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.service.TenantConfigService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.TenantConfigVO;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户端租户配置管理接口
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/configs")
@RequiredArgsConstructor
@SaCheckLogin
public class V1MerchantConfigController {

    private final TenantConfigService tenantConfigService;
    private final V1MerchantSupportService v1MerchantSupportService;

    @GetMapping
    public Result<List<TenantConfigVO>> listConfigs(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        return Result.success(tenantConfigService.listByTenant(tenantId));
    }

    @GetMapping("/{key}")
    public Result<TenantConfigVO> getConfig(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable String key) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        return Result.success(tenantConfigService.getByKey(tenantId, key));
    }

    @PutMapping("/{key}")
    public Result<TenantConfigVO> putConfig(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable String key,
            @RequestBody ConfigUpdateDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        return Result.success(tenantConfigService.put(tenantId, key, dto.getValue()));
    }

    @Data
    public static class ConfigUpdateDTO {
        private String value;
    }
}
