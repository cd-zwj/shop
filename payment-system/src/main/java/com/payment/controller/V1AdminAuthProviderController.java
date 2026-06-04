package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.Result;
import com.payment.dto.PlatformAuthProviderDTO;
import com.payment.dto.PlatformAuthProviderVO;
import com.payment.service.PlatformAuthProviderAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端第三方登录方式管理接口，用于处理管理端第三方登录方式管理的查询、新增、修改或删除等请求。
 */
@RestController
@RequestMapping("/v1/admin/auth-providers")
@RequiredArgsConstructor
public class V1AdminAuthProviderController {

    private final PlatformAuthProviderAdminService platformAuthProviderAdminService;

    /**
     * 查询渠道。
     */
    @SaCheckPermission("admin:auth-provider:list")
    @GetMapping
    public Result<Page<PlatformAuthProviderVO>> listProviders(@RequestParam(defaultValue = "1") Integer current,
        /**
         * 构建成功结果。
         */
                                                              @RequestParam(defaultValue = "10") Integer size,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) Integer status) {
        return Result.success(platformAuthProviderAdminService.listProviders(current, size, keyword, status));
    /**
     * 处理SaCheck权限。
     */
    }

    @SaCheckPermission("admin:auth-provider:detail")
    @GetMapping("/{providerId}")
    public Result<PlatformAuthProviderVO> getProvider(@PathVariable Long providerId) {
        return Result.success(platformAuthProviderAdminService.getProvider(providerId));
    }

    @SaCheckPermission("admin:auth-provider:create")
    @PostMapping
    public Result<PlatformAuthProviderVO> createProvider(@Valid @RequestBody PlatformAuthProviderDTO dto) {
        return Result.success(platformAuthProviderAdminService.createProvider(dto));
    }

    @SaCheckPermission("admin:auth-provider:update")
    @PutMapping("/{providerId}")
    public Result<Void> updateProvider(@PathVariable Long providerId, @Valid @RequestBody PlatformAuthProviderDTO dto) {
        platformAuthProviderAdminService.updateProvider(providerId, dto);
        return Result.success();
    }

    @SaCheckPermission("admin:auth-provider:enable")
    @PutMapping("/{providerId}/enable")
    public Result<Void> enableProvider(@PathVariable Long providerId) {
        platformAuthProviderAdminService.enableProvider(providerId);
        return Result.success();
    }

    @SaCheckPermission("admin:auth-provider:disable")
    @PutMapping("/{providerId}/disable")
    public Result<Void> disableProvider(@PathVariable Long providerId) {
        platformAuthProviderAdminService.disableProvider(providerId);
        return Result.success();
    }
}
