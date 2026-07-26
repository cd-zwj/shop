package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.V1MerchantStoreInventoryAdjustDTO;
import com.payment.dto.V1MerchantStoreInventoryLogVO;
import com.payment.dto.V1MerchantStoreInventoryVO;
import com.payment.service.V1MerchantStoreInventoryService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户门店库存管理接口。
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/inventory")
@RequiredArgsConstructor
public class V1MerchantStoreInventoryController {

    private final V1MerchantStoreInventoryService storeInventoryService;

    @SaCheckLogin(type = "merchant")
    @GetMapping
    public Result<PageResult<V1MerchantStoreInventoryVO>> listStocks(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数必须大于0") Integer size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Boolean lowStockOnly,
            @RequestParam(required = false) Integer threshold) {
        Page<V1MerchantStoreInventoryVO> page = storeInventoryService.listStocks(
                tenantId, PlatformSessionHelper.getPlatformUserId(), current, size,
                storeId, productId, lowStockOnly, threshold);
        return Result.success(PageResult.from(page));
    }

    @SaCheckLogin(type = "merchant")
    @PostMapping("/adjustments")
    public Result<V1MerchantStoreInventoryVO> adjustStock(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @Valid @RequestBody V1MerchantStoreInventoryAdjustDTO dto) {
        return Result.success(storeInventoryService.adjustStock(
                tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckLogin(type = "merchant")
    @GetMapping("/logs")
    public Result<PageResult<V1MerchantStoreInventoryLogVO>> listChangeLogs(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数必须大于0") Integer size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long productId) {
        Page<V1MerchantStoreInventoryLogVO> page = storeInventoryService.listChangeLogs(
                tenantId, PlatformSessionHelper.getPlatformUserId(), current, size, storeId, productId);
        return Result.success(PageResult.from(page));
    }
}
