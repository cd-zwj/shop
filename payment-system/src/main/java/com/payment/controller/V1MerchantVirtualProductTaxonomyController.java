package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.dto.VirtualProductCategoryUpsertDTO;
import com.payment.dto.VirtualProductCategoryVO;
import com.payment.dto.VirtualProductTypeUpsertDTO;
import com.payment.dto.VirtualProductTypeVO;
import com.payment.service.VirtualProductTaxonomyService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}")
@RequiredArgsConstructor
public class V1MerchantVirtualProductTaxonomyController {

    private final VirtualProductTaxonomyService virtualProductTaxonomyService;

    @SaCheckPermission(type = "merchant", value = "merchant:product:read")
    @GetMapping("/virtual-product-types")
    public Result<List<VirtualProductTypeVO>> listTypes(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(required = false) Integer status) {
        return Result.success(virtualProductTaxonomyService.listTypes(
                tenantId, PlatformSessionHelper.getPlatformUserId(), status));
    }

    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @PostMapping("/virtual-product-types")
    public Result<VirtualProductTypeVO> createType(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @Valid @RequestBody VirtualProductTypeUpsertDTO dto) {
        return Result.success(virtualProductTaxonomyService.createType(
                tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @PutMapping("/virtual-product-types/{id}")
    public Result<VirtualProductTypeVO> updateType(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id,
            @Valid @RequestBody VirtualProductTypeUpsertDTO dto) {
        return Result.success(virtualProductTaxonomyService.updateType(
                tenantId, PlatformSessionHelper.getPlatformUserId(), id, dto));
    }

    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @DeleteMapping("/virtual-product-types/{id}")
    public Result<Void> deleteType(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        virtualProductTaxonomyService.deleteType(tenantId, PlatformSessionHelper.getPlatformUserId(), id);
        return Result.success();
    }

    @SaCheckPermission(type = "merchant", value = "merchant:product:read")
    @GetMapping("/virtual-product-categories")
    public Result<List<VirtualProductCategoryVO>> listCategories(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Integer status) {
        return Result.success(virtualProductTaxonomyService.listCategories(
                tenantId, PlatformSessionHelper.getPlatformUserId(), typeId, status));
    }

    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @PostMapping("/virtual-product-categories")
    public Result<VirtualProductCategoryVO> createCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @Valid @RequestBody VirtualProductCategoryUpsertDTO dto) {
        return Result.success(virtualProductTaxonomyService.createCategory(
                tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @PutMapping("/virtual-product-categories/{id}")
    public Result<VirtualProductCategoryVO> updateCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id,
            @Valid @RequestBody VirtualProductCategoryUpsertDTO dto) {
        return Result.success(virtualProductTaxonomyService.updateCategory(
                tenantId, PlatformSessionHelper.getPlatformUserId(), id, dto));
    }

    @SaCheckPermission(type = "merchant", value = "merchant:product:write")
    @DeleteMapping("/virtual-product-categories/{id}")
    public Result<Void> deleteCategory(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        virtualProductTaxonomyService.deleteCategory(tenantId, PlatformSessionHelper.getPlatformUserId(), id);
        return Result.success();
    }
}
