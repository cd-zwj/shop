package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantStoreUpsertDTO;
import com.payment.dto.V1MerchantStoreVO;
import com.payment.service.V1MerchantStoreService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商户端门店管理控制器（Merchant 端）。
 * <p>提供商户对门店信息的 CRUD 操作以及门店状态的启停管理。
 * 需要商户角色登录，并通过商户员工本地权限矩阵控制访问。</p>
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/stores")
@RequiredArgsConstructor
public class V1MerchantStoreController {

    private final V1MerchantStoreService v1MerchantStoreService;
    private final V1MerchantSupportService v1MerchantSupportService;

    @SaCheckLogin(type = "merchant")
    @GetMapping
    public Result<PageResult<V1MerchantStoreVO>> listStores(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Long platformUserId = requireStorePermission(tenantId);
        Page<V1MerchantStoreVO> page = v1MerchantStoreService.listStores(
                tenantId, platformUserId, current, size, keyword, status);
        return Result.success(PageResult.from(page));
    }

    @SaCheckLogin(type = "merchant")
    @GetMapping("/{storeId}")
    public Result<V1MerchantStoreVO> getStore(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long storeId) {
        Long platformUserId = requireStorePermission(tenantId);
        return Result.success(v1MerchantStoreService.getStore(tenantId, platformUserId, storeId));
    }

    @SaCheckLogin(type = "merchant")
    @PostMapping
    public Result<V1MerchantStoreVO> createStore(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @Valid @RequestBody V1MerchantStoreUpsertDTO dto) {
        Long platformUserId = requireStorePermission(tenantId);
        return Result.success(v1MerchantStoreService.createStore(tenantId, platformUserId, dto));
    }

    @SaCheckLogin(type = "merchant")
    @PutMapping("/{storeId}")
    public Result<V1MerchantStoreVO> updateStore(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long storeId,
            @Valid @RequestBody V1MerchantStoreUpsertDTO dto) {
        Long platformUserId = requireStorePermission(tenantId);
        return Result.success(v1MerchantStoreService.updateStore(tenantId, platformUserId, storeId, dto));
    }

    @SaCheckLogin(type = "merchant")
    @PutMapping("/{storeId}/status")
    public Result<V1MerchantStoreVO> updateStoreStatus(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long storeId,
            @RequestParam @Min(value = 0, message = "状态只能为0或1") @Max(value = 1, message = "状态只能为0或1") Integer status) {
        Long platformUserId = requireStorePermission(tenantId);
        return Result.success(v1MerchantStoreService.updateStoreStatus(
                tenantId, platformUserId, storeId, status));
    }

    @SaCheckLogin(type = "merchant")
    @DeleteMapping("/{storeId}")
    public Result<Void> deleteStore(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long storeId) {
        Long platformUserId = requireStorePermission(tenantId);
        v1MerchantStoreService.deleteStore(tenantId, platformUserId, storeId);
        return Result.success();
    }

    private Long requireStorePermission(Long tenantId) {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.STORE_MANAGE);
        return platformUserId;
    }
}
