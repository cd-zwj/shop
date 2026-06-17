package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.V1MerchantStoreUpsertDTO;
import com.payment.dto.V1MerchantStoreVO;
import com.payment.service.V1MerchantStoreService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/stores")
@RequiredArgsConstructor
public class V1MerchantStoreController {

    private final V1MerchantStoreService v1MerchantStoreService;

    @SaCheckPermission("merchant:store:read")
    @GetMapping
    public Result<PageResult<V1MerchantStoreVO>> listStores(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Page<V1MerchantStoreVO> page = v1MerchantStoreService.listStores(
                tenantId, PlatformSessionHelper.getPlatformUserId(), current, size, keyword, status);
        return Result.success(PageResult.from(page));
    }

    @SaCheckPermission("merchant:store:read")
    @GetMapping("/{storeId}")
    public Result<V1MerchantStoreVO> getStore(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long storeId) {
        return Result.success(v1MerchantStoreService.getStore(tenantId, PlatformSessionHelper.getPlatformUserId(), storeId));
    }

    @SaCheckPermission("merchant:store:write")
    @PostMapping
    public Result<V1MerchantStoreVO> createStore(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @Valid @RequestBody V1MerchantStoreUpsertDTO dto) {
        return Result.success(v1MerchantStoreService.createStore(tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckPermission("merchant:store:write")
    @PutMapping("/{storeId}")
    public Result<V1MerchantStoreVO> updateStore(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long storeId,
            @Valid @RequestBody V1MerchantStoreUpsertDTO dto) {
        return Result.success(v1MerchantStoreService.updateStore(tenantId, PlatformSessionHelper.getPlatformUserId(), storeId, dto));
    }

    @SaCheckPermission("merchant:store:write")
    @PutMapping("/{storeId}/status")
    public Result<V1MerchantStoreVO> updateStoreStatus(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long storeId,
            @RequestParam @Min(value = 0, message = "状态只能为0或1") @Max(value = 1, message = "状态只能为0或1") Integer status) {
        return Result.success(v1MerchantStoreService.updateStoreStatus(
                tenantId, PlatformSessionHelper.getPlatformUserId(), storeId, status));
    }

    @SaCheckPermission("merchant:store:write")
    @DeleteMapping("/{storeId}")
    public Result<Void> deleteStore(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long storeId) {
        v1MerchantStoreService.deleteStore(tenantId, PlatformSessionHelper.getPlatformUserId(), storeId);
        return Result.success();
    }
}
